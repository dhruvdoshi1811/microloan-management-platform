package com.dhruv.microloan_platform;

import com.dhruv.microloan_platform.dto.auth.AuthResponse;
import com.dhruv.microloan_platform.dto.auth.LoginRequest;
import com.dhruv.microloan_platform.dto.auth.RegisterRequest;
import com.dhruv.microloan_platform.dto.borrower.BorrowerRequest;
import com.dhruv.microloan_platform.dto.borrower.BorrowerResponse;
import com.dhruv.microloan_platform.dto.kyc.KycInitiateRequest;
import com.dhruv.microloan_platform.dto.kyc.KycResponse;
import com.dhruv.microloan_platform.dto.kyc.OtpInitiateResponse;
import com.dhruv.microloan_platform.dto.kyc.OtpVerifyRequest;
import com.dhruv.microloan_platform.dto.loan.InstallmentResponse;
import com.dhruv.microloan_platform.dto.loan.LoanResponse;
import com.dhruv.microloan_platform.dto.loanapplication.LoanApplicationRequest;
import com.dhruv.microloan_platform.dto.loanapplication.LoanApplicationResponse;
import com.dhruv.microloan_platform.dto.loanapplication.RejectRequest;
import com.dhruv.microloan_platform.dto.loanproduct.LoanProductRequest;
import com.dhruv.microloan_platform.dto.loanproduct.LoanProductResponse;
import com.dhruv.microloan_platform.dto.repayment.RepaymentRequest;
import com.dhruv.microloan_platform.dto.repayment.RepaymentResponse;
import com.dhruv.microloan_platform.entity.ApplicationStatus;
import com.dhruv.microloan_platform.entity.DocumentType;
import com.dhruv.microloan_platform.entity.InstallmentStatus;
import com.dhruv.microloan_platform.entity.KycLevel;
import com.dhruv.microloan_platform.entity.LoanStatus;
import com.dhruv.microloan_platform.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FullLoanLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullLifecycle_applicationThroughApprovalAcknowledgementRepaymentAndClosure() throws Exception {
        String adminToken = registerAndLogin(Role.ADMIN);
        String borrowerUserToken = registerAndLogin(Role.BORROWER);

        Long productId = createLoanProduct(adminToken);
        Long borrowerId = createBorrowerWithBasicKyc(borrowerUserToken);

        LoanApplicationRequest applicationRequest = new LoanApplicationRequest(
                borrowerId, productId, new BigDecimal("60000"), 12);
        LoanApplicationResponse application = postForObject("/loan-applications", applicationRequest,
                borrowerUserToken, LoanApplicationResponse.class, status().isCreated());
        assertThat(application.status()).isEqualTo(ApplicationStatus.PENDING);

        LoanApplicationResponse approved = postForObject(
                "/loan-applications/" + application.id() + "/approve", null,
                adminToken, LoanApplicationResponse.class, status().isOk());
        assertThat(approved.status()).isEqualTo(ApplicationStatus.APPROVED);

        JsonNode loanNode = findInPage("/loans", adminToken, "applicationId", application.id());
        Long loanId = loanNode.get("id").asLong();
        assertThat(loanNode.get("status").asText()).isEqualTo(LoanStatus.AGREEMENT_PENDING.name());

        LoanResponse activeLoan = postForObject("/loans/" + loanId + "/agreement/acknowledge", null,
                borrowerUserToken, LoanResponse.class, status().isOk());
        assertThat(activeLoan.status()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(activeLoan.disbursedAt()).isNotNull();
        assertThat(activeLoan.agreementAcknowledgedAt()).isNotNull();

        InstallmentResponse[] installments = getForObject("/loans/" + loanId + "/installments",
                borrowerUserToken, InstallmentResponse[].class);
        assertThat(installments).hasSize(12);
        assertThat(installments).allSatisfy(installment ->
                assertThat(installment.status()).isEqualTo(InstallmentStatus.PENDING));

        BigDecimal totalPayable = activeLoan.totalPayable();
        RepaymentRequest repaymentRequest = new RepaymentRequest(
                loanId, totalPayable, "PAYREF-" + UUID.randomUUID(), "UPI");
        RepaymentResponse repayment = postForObject("/repayments", repaymentRequest,
                borrowerUserToken, RepaymentResponse.class, status().isCreated());
        assertThat(repayment.amount()).isEqualByComparingTo(totalPayable);
        assertThat(repayment.balanceAfter()).isEqualByComparingTo(BigDecimal.ZERO);

        LoanResponse closedLoan = getForObject("/loans/" + loanId, borrowerUserToken, LoanResponse.class);
        assertThat(closedLoan.status()).isEqualTo(LoanStatus.CLOSED);
        assertThat(closedLoan.totalPaid()).isEqualByComparingTo(totalPayable);

        InstallmentResponse[] paidInstallments = getForObject("/loans/" + loanId + "/installments",
                borrowerUserToken, InstallmentResponse[].class);
        assertThat(paidInstallments).allSatisfy(installment ->
                assertThat(installment.status()).isEqualTo(InstallmentStatus.PAID));

        JsonNode approvedEvent = findInPage("/admin/outbox", adminToken, "aggregateId", loanId,
                node -> "LOAN_APPROVED".equals(node.get("eventType").asText()));
        assertThat(approvedEvent).isNotNull();
        JsonNode repaymentEvent = findInPage("/admin/outbox", adminToken, "aggregateId", loanId,
                node -> "REPAYMENT_RECEIVED".equals(node.get("eventType").asText()));
        assertThat(repaymentEvent).isNotNull();
    }

    @Test
    void applicationRejection_stopsAtRejectedStatusWithReasonRecorded() throws Exception {
        String adminToken = registerAndLogin(Role.ADMIN);
        String borrowerUserToken = registerAndLogin(Role.BORROWER);

        Long productId = createLoanProduct(adminToken);
        Long borrowerId = createBorrowerWithBasicKyc(borrowerUserToken);

        LoanApplicationRequest applicationRequest = new LoanApplicationRequest(
                borrowerId, productId, new BigDecimal("60000"), 12);
        LoanApplicationResponse application = postForObject("/loan-applications", applicationRequest,
                borrowerUserToken, LoanApplicationResponse.class, status().isCreated());

        RejectRequest rejectRequest = new RejectRequest("Debt-to-income ratio too high");
        LoanApplicationResponse rejected = postForObject(
                "/loan-applications/" + application.id() + "/reject", rejectRequest,
                adminToken, LoanApplicationResponse.class, status().isOk());

        assertThat(rejected.status()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(rejected.rejectionReason()).isEqualTo("Debt-to-income ratio too high");
    }

    private String registerAndLogin(Role role) throws Exception {
        String email = role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.com";
        String password = "Password123!";

        postForObject("/auth/register", new RegisterRequest(email, password, role),
                null, Object.class, status().isCreated());

        AuthResponse auth = postForObject("/auth/login", new LoginRequest(email, password),
                null, AuthResponse.class, status().isOk());
        return auth.token();
    }

    private Long createLoanProduct(String adminToken) throws Exception {
        LoanProductRequest request = new LoanProductRequest(
                "Integration Test Personal Loan " + UUID.randomUUID(),
                new BigDecimal("10000"), new BigDecimal("500000"),
                6, 36, new BigDecimal("12.00"), new BigDecimal("2.00"), KycLevel.BASIC);
        LoanProductResponse product = postForObject("/loan-products", request, adminToken,
                LoanProductResponse.class, status().isCreated());
        return product.id();
    }

    private Long createBorrowerWithBasicKyc(String borrowerUserToken) throws Exception {
        String email = "jane-" + UUID.randomUUID() + "@example.com";
        BorrowerRequest borrowerRequest = new BorrowerRequest(
                "Jane Doe", "9876543210", email, LocalDate.of(1990, 1, 1), new BigDecimal("50000"));
        BorrowerResponse borrower = postForObject("/borrowers", borrowerRequest, borrowerUserToken,
                BorrowerResponse.class, status().isCreated());
        assertThat(borrower.kycLevel()).isEqualTo(KycLevel.NONE);

        String panNumber = UUID.randomUUID().toString().replace("-", "").toUpperCase().substring(0, 10);
        KycInitiateRequest initiateRequest = new KycInitiateRequest(DocumentType.PAN, panNumber);
        OtpInitiateResponse otp = postForObject("/borrowers/" + borrower.id() + "/kyc/initiate",
                initiateRequest, borrowerUserToken, OtpInitiateResponse.class, status().isCreated());

        OtpVerifyRequest verifyRequest = new OtpVerifyRequest(DocumentType.PAN, otp.otpCode());
        KycResponse kyc = postForObject("/borrowers/" + borrower.id() + "/kyc/verify-otp",
                verifyRequest, borrowerUserToken, KycResponse.class, status().isOk());
        assertThat(kyc.kycLevel()).isEqualTo(KycLevel.BASIC);

        return borrower.id();
    }

    private JsonNode findInPage(String url, String token, String field, long value) throws Exception {
        return findInPage(url, token, field, value, node -> true);
    }

    private JsonNode findInPage(String url, String token, String field, long value,
                                 java.util.function.Predicate<JsonNode> extra) throws Exception {
        JsonNode page = getForObject(url + (url.contains("?") ? "&" : "?") + "size=1000",
                token, JsonNode.class);
        JsonNode content = page.get("content");
        for (JsonNode node : content) {
            if (node.get(field).asLong() == value && extra.test(node)) {
                return node;
            }
        }
        throw new NoSuchElementException("No element in " + url + " with " + field + "=" + value);
    }

    private <T> T postForObject(String url, Object body, String token, Class<T> responseType,
                                 org.springframework.test.web.servlet.ResultMatcher expectedStatus) throws Exception {
        var request = post(url).contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            request = request.content(objectMapper.writeValueAsString(body));
        } else {
            request = request.content("");
        }
        if (token != null) {
            request = request.header("Authorization", "Bearer " + token);
        }
        MvcResult result = mockMvc.perform(request).andExpect(expectedStatus).andReturn();
        if (responseType == Object.class) {
            return null;
        }
        return objectMapper.readValue(result.getResponse().getContentAsByteArray(), responseType);
    }

    private <T> T getForObject(String url, String token, Class<T> responseType) throws Exception {
        var request = get(url);
        if (token != null) {
            request = request.header("Authorization", "Bearer " + token);
        }
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsByteArray(), responseType);
    }
}
