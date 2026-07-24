-- Phase E retrofit: penaltyRate was frozen into agreementSnapshot JSON in Phase C but never
-- promoted to a real column, unlike interestRate/emiAmount/totalPayable. The overdue batch
-- job needs to compute penalties without parsing JSON, so it gets a proper column now.
ALTER TABLE loans ADD COLUMN penalty_rate NUMERIC(5, 2) NOT NULL DEFAULT 0;
