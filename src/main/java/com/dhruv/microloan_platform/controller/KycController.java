package com.dhruv.microloan_platform.controller;

import com.dhruv.microloan_platform.dto.kyc.KycInitiateRequest;
import com.dhruv.microloan_platform.dto.kyc.KycResponse;
import com.dhruv.microloan_platform.dto.kyc.OtpInitiateResponse;
import com.dhruv.microloan_platform.dto.kyc.OtpVerifyRequest;
import com.dhruv.microloan_platform.service.KycService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/borrowers/{borrowerId}/kyc")
public class KycController {

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    @PostMapping("/initiate")
    public ResponseEntity<OtpInitiateResponse> initiate(@PathVariable Long borrowerId,
                                                          @Valid @RequestBody KycInitiateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(kycService.initiate(borrowerId, request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<KycResponse> verifyOtp(@PathVariable Long borrowerId,
                                                  @Valid @RequestBody OtpVerifyRequest request) {
        return ResponseEntity.ok(kycService.verifyOtp(borrowerId, request));
    }

    @GetMapping
    public ResponseEntity<KycResponse> getKyc(@PathVariable Long borrowerId) {
        return ResponseEntity.ok(kycService.getKyc(borrowerId));
    }
}
