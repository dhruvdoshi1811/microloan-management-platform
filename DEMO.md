# Demo checklist

A click-through script for recording a short demo, mirroring exactly what
`FullLoanLifecycleIntegrationTest` proves against real HTTP - if something here doesn't work,
that test (or its rejection-branch counterpart) is the first place to look.

Have both the backend and frontend running first (`docker compose up --build` +
`cd frontend && npm run dev`, or the deployed Render URLs - see `DEPLOYMENT.md`). If demoing the
deployed version, hit the backend once beforehand so it isn't waking from an idle sleep on camera.

## 1. Admin creates an account

1. Open the frontend, switch to **Register**.
2. Register with role **Admin**. You're logged in immediately.
3. Log out (top-right).

## 2. Borrower registers and sets up a profile

1. Register a second account with role **Borrower**. Logged in as borrower now.
2. On first login there's no Borrower ID yet - fill in the **"Create a borrower profile"** form
   (name, phone, email, DOB, monthly income). This is deliberately a separate step from the
   login account - see the README's note on why `User` and `Borrower` aren't linked.

## 3. Complete KYC

1. The page shows a KYC panel since the new borrower's level is `NONE`.
2. Pick **PAN**, enter any document number, click **Send OTP**.
3. The OTP code comes back directly in the response and is shown/auto-filled - point this out as
   a deliberate dev-only shortcut (no real SMS/UIDAI gateway exists here), not an oversight.
4. Click **Verify OTP**. KYC level becomes `BASIC`.

## 4. Apply for a loan

1. The demo seed migration already created one `LoanProduct` ("Personal Loan") - it shows up in
   the product picker.
2. Enter an amount and tenure within the product's range, submit.
3. The new application shows up under "My applications" with status `PENDING`.

## 5. Admin approves it

1. Log out, log back in as the admin account.
2. The pending application is listed. Click **Approve**.
3. Scroll to **Outbox events** - a `LOAN_APPROVED` event now exists for this loan. Point out that
   this row was written in the same transaction as the approval, whether or not the scheduled
   publisher has run yet.

## 6. Borrower acknowledges and gets an installment schedule

1. Log back in as the borrower.
2. Under "My loans" the loan shows status `AGREEMENT_PENDING`. Click **Acknowledge agreement**.
3. Status flips to `ACTIVE`. Click **View installments** - the full monthly schedule appears,
   generated at acknowledgement time, not at approval.

## 7. Make a repayment

1. Enter the loan's full outstanding amount (shown on the loan card) and a payment mode, submit.
2. The loan's status flips to `CLOSED` and outstanding drops to zero.
3. Reload installments - every one now shows `PAID`.

## 8. Back to admin: confirm the second event and (optionally) run the overdue check

1. Log back in as admin. The outbox now also shows a `REPAYMENT_RECEIVED` event for this loan.
2. Click **Run overdue check** to show the manually-triggerable version of the daily batch job -
   the summary (loans scanned / marked overdue / installments overdue / penalties applied) prints
   on screen. On a loan this fresh it'll report zero overdue, which is itself worth narrating:
   the job ran safely against real data and found nothing to do.

## Optional: show the rejection branch

Submit a second application and reject it as admin instead of approving, to show the other
branch (`REJECTED` status with a recorded reason) - covered by
`applicationRejection_stopsAtRejectedStatusWithReasonRecorded` in the same integration test.
