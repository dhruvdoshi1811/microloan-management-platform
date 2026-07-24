INSERT INTO loan_products (
    name, min_principal, max_principal, min_tenure_months, max_tenure_months,
    interest_rate, penalty_rate, min_kyc_level, is_active
) VALUES (
    'Personal Loan', 10000.00, 500000.00, 6, 36,
    12.00, 2.00, 'BASIC', TRUE
);
