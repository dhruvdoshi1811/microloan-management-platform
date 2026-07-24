# Microloan Management Platform

A rule-driven digital lending backend covering the full loan lifecycle — onboarding, KYC,
application, approval, disbursement, repayment, and closure — built as a single
well-modularized Spring Boot app, plus a minimal React frontend that demos the whole flow.

## Architecture

```mermaid
flowchart LR
    subgraph Client
        FE["React + Tailwind frontend<br/>(borrower view / admin view)"]
    end

    subgraph Backend["Spring Boot app - one deployable"]
        SEC["JWT auth filter<br/>+ Spring Security"]
        CTRL["Controllers<br/>(REST, ~30 endpoints)"]
        SVC["Services<br/>(rule engine, EMI calc,<br/>repayment allocation)"]
        REPO["Spring Data JPA<br/>repositories"]
        SCHED["Scheduled jobs<br/>(outbox publisher,<br/>daily overdue check)"]
    end

    DB[("PostgreSQL<br/>(H2 in tests)")]

    FE -- "HTTPS/JSON + Bearer JWT" --> SEC --> CTRL --> SVC --> REPO --> DB
    SCHED --> REPO
    SCHED -.polls PENDING OutboxEvent rows.-> REPO
```

The frontend and backend deploy as two separate pieces (a static site + a web service), not
bundled into one image — CORS is enabled on the backend specifically for this. See
[`DEPLOYMENT.md`](DEPLOYMENT.md) for how that maps onto Render.

## Tech stack

- **Backend:** Java 17, Spring Boot 4.1, Spring Data JPA / Hibernate, Spring Security (JWT), Flyway
- **DB:** PostgreSQL in production/dev, H2 (Postgres-compatibility mode) in tests
- **Testing:** JUnit 5, Mockito, AssertJ, `@WebMvcTest`/MockMvc for controller slices, one true
  `@SpringBootTest` + `@AutoConfigureMockMvc` end-to-end test for the full lifecycle
- **Frontend:** React (Vite), Tailwind CSS v4 - no router, no state library, plain `fetch`
- **Containerization/Deploy:** Docker (multi-stage build), Docker Compose for local dev, Render
  for the live deployment

## Engineering patterns

Each of these is a deliberate choice, not the only way to build this - see the file for the
reasoning in context.

1. **Pessimistic locking on repayment processing.** A repayment locks the `Loan` and its unpaid
   `Installment` rows (`SELECT ... FOR UPDATE`) before allocating funds FIFO, since retrying a
   failed money allocation is expensive and confusing, but a short lock wait isn't.
   → `service/RepaymentService.java`
2. **Optimistic locking on application/loan state transitions.** `@Version` on `LoanApplication`
   and `Loan` - contrast with #1: used where conflicts are rare and a retry is cheap.
   → `entity/LoanApplication.java`, `entity/Loan.java`
3. **Immutable agreement snapshot.** Approving a loan freezes its terms (principal, rate,
   tenure, computed EMI) into a JSON snapshot, so a later `LoanProduct` rate change never
   silently changes an existing loan's terms.
   → `service/LoanApplicationService.java` (`buildLoan`), `dto/loan/AgreementSnapshot.java`
4. **Idempotent repayment & penalty processing.** `paymentReference` has a unique constraint and
   doubles as an idempotency key; `Installment.penaltyApplied` stops a re-run overdue job from
   charging a penalty twice.
   → `service/RepaymentService.java`, `service/OverdueService.java`
5. **Configurable rule engine.** Loan eligibility (principal range, tenure range, minimum KYC
   level, EMI-to-income ratio) is a list of `EligibilityRule` beans, not hardcoded if/else.
   → `service/eligibility/`
6. **Transactional outbox pattern.** State changes that need to notify (approved, repaid,
   overdue) write an `OutboxEvent` row in the same transaction as the state change; a separate
   scheduled poller publishes it. Nothing is lost on a crash between the two.
   → `service/OutboxEventWriter.java`, `service/OutboxPublisher.java`
7. **Scheduled batch job.** A daily job pages through active loans, marks overdue installments,
   and applies penalties idempotently.
   → `service/OverdueService.java`

## Project layout

```
src/main/java/.../
  entity/        JPA entities
  repository/     Spring Data repositories
  service/        business logic (incl. eligibility/ rule engine)
  controller/     REST controllers
  dto/            request/response records
  security/       JWT filter, Spring Security config
  exception/      global exception handling
src/main/resources/db/migration/   Flyway migrations (V1-V7)
src/test/java/...                  unit tests, controller slices, one full-lifecycle e2e test
frontend/                          React + Tailwind UI (separate npm project)
Dockerfile, docker-compose.yml     backend containerization
render.yaml, DEPLOYMENT.md         deployment
DEMO.pdf                           demo walkthrough with screenshots
```

One deliberate gap worth knowing about: `User` (login/auth) and `Borrower` (the loan-domain
profile) are **not linked** to each other. Registering a `BORROWER` user just proves you can log
in; you separately create or look up a `Borrower` by ID to actually apply for a loan. The
frontend's "enter your Borrower ID / create one" step is an honest reflection of this, not a
workaround.

## Getting started

### Backend, via Docker Compose (recommended)

```bash
docker compose up --build
```

This runs Postgres + the Spring Boot app together, migrates the schema, and seeds one demo
`LoanProduct` (see `V7__seed_demo_loan_product.sql`). The API is then at `http://localhost:8080`.

### Backend, without Docker

Requires a local Postgres matching the `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` defaults in
`application.properties` (or override them via environment variables), and a JDK matching
`java.version` in `pom.xml`.

```bash
./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
cp .env.example .env   # VITE_API_BASE_URL should point at the backend above
npm run dev
```

Opens at `http://localhost:5173` (the backend's default CORS-allowed origin).

## Running the tests

```bash
./mvnw test
```

Covers unit tests, controller slice tests (`@WebMvcTest`), a couple of concurrency-focused
tests that deliberately break and restore a guarantee to prove it holds (see
`RepaymentConcurrencyTest`, `OutboxAtomicityTest`), and `FullLoanLifecycleIntegrationTest` - a
real-HTTP, real-JWT, real-H2-database test of the entire application → approval →
acknowledgement → repayment → closure lifecycle, plus a rejection branch.

## API overview

~30 endpoints across auth, borrower onboarding, KYC, loan products, loan applications, loan
lifecycle, repayments, and admin/observability - read the `controller/` package for the full
map, each controller is small and single-purpose.

## Deployment & demo

- [`DEPLOYMENT.md`](DEPLOYMENT.md) - deploying to Render (recommended) or Railway
- [`DEMO.pdf`](DEMO.pdf) - a walkthrough of the full lifecycle with screenshots
