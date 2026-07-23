# Microloan Management Platform — Build Spec

A rule-driven digital lending backend covering the full loan lifecycle: onboarding, KYC, application, approval, disbursement, repayment, and closure. This document is the source of truth to hand Claude Code, phase by phase. Read the whole thing once before starting Phase A.

**Note:** all naming here is generic/original — no proprietary infrastructure names, no references to any specific company's internal tooling. Keep it that way if you extend this.

---

## 1. Tech Stack

- **Backend:** Java 17, Spring Boot 4.1, Spring Data JPA / Hibernate, Spring Security (JWT)
- **DB:** PostgreSQL (H2 for local/test)
- **Migrations:** Flyway
- **Build:** Maven
- **Testing:** JUnit 5, Mockito, `@WebMvcTest`/MockMvc for controllers
- **Containerization/Deploy:** Docker → Render or Railway
- **Frontend:** React + Tailwind (minimal, dashboard-style)

Keep this a **single well-modularized Spring Boot app**, not multiple microservices. The event-driven notification pattern (Section 3) still teaches the real decoupling concept without needing real inter-service infrastructure. Splitting into a second service is an optional stretch goal only if Phase F finishes early.

---

## 2. Domain Model

| Entity | Key Fields |
|---|---|
| `Borrower` | id, fullName, phone, email, dob, monthlyIncome, kycLevel (NONE/BASIC/FULL), isActive |
| `KycRecord` | id, borrowerId, panNumber (unique), aadhaarNumber (unique), panVerified, aadhaarVerified |
| `OtpVerification` | id, borrowerId, documentType, otpCode, expiresAt, verified, attempts |
| `LoanProduct` | id, name, minPrincipal, maxPrincipal, minTenureMonths, maxTenureMonths, interestRate, penaltyRate, minKycLevel, isActive |
| `LoanApplication` | id, borrowerId, productId, requestedAmount, requestedTenureMonths, status (PENDING/APPROVED/REJECTED), rejectionReason, `@Version` |
| `Loan` | id, borrowerId, applicationId, principalAmount, interestRate, tenureMonths, emiAmount, totalPayable, totalPaid, status (AGREEMENT_PENDING/ACTIVE/OVERDUE/CLOSED), **agreementSnapshot (JSON, frozen at approval)**, agreementAcknowledgedAt, disbursedAt, `@Version` |
| `Installment` | id, loanId, installmentNo, dueDate, emiAmount, penaltyAmount, totalDue, amountPaid, status (PENDING/PARTIAL/PAID/OVERDUE), penaltyApplied (boolean) |
| `Repayment` | id, loanId, amount, paymentReference (unique — doubles as idempotency key), paymentMode, balanceAfter, paidAt |
| `OutboxEvent` | id, aggregateType, aggregateId, eventType, payload (JSON), status (PENDING/PUBLISHED), createdAt |
| `User` | id, email, passwordHash, role — auth |

---

## 3. Engineering Patterns (the actual point of this project)

Every one of these needs to be something **you** can explain unprompted — that's what makes this interview-defensible.

1. **Pessimistic locking on repayment processing.** When a repayment comes in, lock the `Loan` and its unpaid `Installment` rows (`@Lock(LockModeType.PESSIMISTIC_WRITE)` or `SELECT ... FOR UPDATE`) before allocating funds FIFO across installments. This guarantees correctness when concurrent repayments target the same loan — chosen deliberately over optimistic locking because retrying a failed repayment allocation is expensive and confusing for a user, whereas a short wait for a lock is not.
2. **Optimistic locking on application/loan state transitions.** `@Version` on `LoanApplication` and `Loan` — contrast this with #1 in your own head: use where conflicts are rare and a retry is cheap (e.g., two admins approving the same application).
3. **Immutable agreement snapshot.** When a loan is approved, freeze the terms (principal, rate, tenure, computed EMI) into a JSON snapshot on `Loan`. Even if `LoanProduct` rates change later, the loan's terms never silently change. This is a real "why immutability matters" story for an interview.
4. **Idempotent repayment & penalty processing.** `paymentReference` unique constraint prevents double-processing a retried repayment request. `Installment.penaltyApplied` boolean prevents a re-run overdue job from charging a penalty twice.
5. **Configurable rule engine.** `LoanProduct` constraints (principal range, tenure range, minimum KYC level, EMI-to-income ratio) drive eligibility checks — not hardcoded if/else.
6. **Transactional outbox pattern.** State changes that need to notify (loan approved, repayment received, loan overdue) write an `OutboxEvent` row in the same DB transaction as the state change; a separate scheduled poller publishes and marks it sent. No event lost on crash.
7. **Scheduled batch job.** A daily `@Scheduled` job pages through active loans, marks overdue installments, applies penalties (idempotently), and writes outbox events — a real batch-processing pattern, not a toy loop.

---

## 4. Endpoint Map (~30 endpoints)

**Auth**
- `POST /auth/register`, `POST /auth/login`, `GET /auth/me`

**Borrower**
- `POST /borrowers`, `GET /borrowers/{id}`, `PUT /borrowers/{id}`, `GET /borrowers`

**KYC**
- `POST /borrowers/{id}/kyc/initiate`
- `POST /borrowers/{id}/kyc/verify-otp`
- `GET /borrowers/{id}/kyc`

**Loan Products (admin)**
- `POST /loan-products`, `GET /loan-products`, `GET /loan-products/{id}`, `PUT /loan-products/{id}`

**Loan Applications**
- `POST /loan-applications` — runs full rule-engine eligibility check
- `GET /loan-applications`, `GET /loan-applications/{id}`
- `POST /loan-applications/{id}/approve` — generates agreement snapshot, creates `Loan` in AGREEMENT_PENDING
- `POST /loan-applications/{id}/reject`

**Loan Lifecycle**
- `GET /loans`, `GET /loans/{id}`, `GET /loans/{id}/installments`
- `POST /loans/{id}/agreement/acknowledge` — borrower accepts terms → generates installment schedule → status ACTIVE → disbursal

**Repayments (core module)**
- `POST /repayments` — requires `paymentReference`; pessimistic-locked FIFO allocation
- `GET /loans/{id}/repayments`, `GET /repayments/{id}`

**Admin / Observability**
- `POST /admin/run-overdue-check` — manually triggers the scheduled job's logic, for demo purposes
- `GET /admin/outbox?status=PENDING`

---

## 5. Build Order (dependency-driven, not calendar-driven)

Work through these in order — each depends on the previous one existing. **Do not skip the review ritual in Section 6 between phases.**

**Phase A — Foundation**
Project scaffold, Flyway baseline, `Borrower`/`User`/`KycRecord`/`OtpVerification` entities + repositories + controllers, JWT auth, layered architecture, global exception handling, initial unit tests.

**Phase B — Loan Products & Rule-Driven Applications**
`LoanProduct` CRUD, `LoanApplication` submission with full eligibility rule engine (principal range, tenure range, KYC level, EMI-to-income ratio), approve/reject endpoints.

**Phase C — Agreement & Disbursement**
Immutable agreement snapshot generation on approval, `@Version` optimistic locking on `Loan`/`LoanApplication`, acknowledgement endpoint that generates the `Installment` schedule and activates the loan.

**Phase D — Repayment & Concurrency Core**
`Repayment` processing with pessimistic locking + FIFO allocation across installments, idempotency via `paymentReference`. Write a concurrency test: fire two simultaneous repayments at the same loan, prove allocation stays correct and consistent.

**Phase E — Overdue Detection & Event-Driven Notifications**
`OutboxEvent` entity, outbox-write-in-same-transaction pattern, scheduled publisher job, the daily overdue-detection batch job with idempotent penalty application. Prove it: kill the app mid-transaction (or simulate), restart, confirm no event lost, no double-published.

**Phase F — Deployment & Frontend**
Dockerfile, deploy to Render/Railway, minimal React + Tailwind dashboard (borrower view: apply for loan, see repayment schedule, make a repayment; admin view: approve applications, see outbox/overdue status), integration tests, README with architecture diagram, demo GIF.

---

## 6. The Review Ritual (do this after every phase, no exceptions)

1. Start the phase in **Plan Mode** (`Shift+Tab` in Claude Code). Read the full plan before approving.
2. Ask at least 2 "why" questions before approving execution (e.g., "why pessimistic locking here but optimistic locking on the application status?").
3. After execution: *"Walk me through every file you just created or changed, in the order I should read them, and explain what each does and why."*
4. Prompt: *"Quiz me on [the feature you just built] — ask me to explain the flow step by step and correct me if I'm wrong."*
5. Only move to the next phase once you can explain the current one out loud without looking at the code.

---

## 7. Resume Bullet Targets

- *"Designed a repayment allocation engine using pessimistic locking to guarantee correct FIFO installment settlement under concurrent repayment attempts, contrasted with optimistic locking on lower-contention state transitions."*
- *"Implemented an immutable loan agreement snapshot pattern, freezing terms at approval to guarantee regulatory-style consistency regardless of later product changes."*
- *"Built a configurable loan-eligibility rule engine (KYC level, EMI-to-income ratio, principal/tenure bounds) decoupling business rules from code."*
- *"Implemented the transactional outbox pattern for reliable event publishing, paired with a scheduled batch job for idempotent overdue detection and penalty application."*