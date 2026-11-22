-- ============================================================================
-- Infinity Library Management System - Database Initialization
-- Schema Version: 1.0 (Flyway V1)
-- Standards: ISO/IEC 9075, ANSI SQL-92, OWASP ASVS 5.0.0, RFC 9457
-- ============================================================================

-- 1. Libraries (Institutional Configuration)
CREATE TABLE libraries (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    address VARCHAR(500),
    email VARCHAR(255),
    phone VARCHAR(50),
    timezone VARCHAR(100) NOT NULL DEFAULT 'UTC',
    loan_duration_days INT NOT NULL DEFAULT 14,
    max_loans_per_member INT NOT NULL DEFAULT 5,
    daily_fine_rate NUMERIC(10, 2) NOT NULL DEFAULT 5.00,
    max_fine_per_loan NUMERIC(10, 2) NOT NULL DEFAULT 500.00,
    reservation_hold_hours INT NOT NULL DEFAULT 48,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Books (Bibliographic Titles)
CREATE TABLE books (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    isbn VARCHAR(20) NOT NULL UNIQUE,
    author VARCHAR(255) NOT NULL,
    publisher VARCHAR(255),
    publication_year INT,
    genre VARCHAR(100) NOT NULL,
    language VARCHAR(50) NOT NULL DEFAULT 'English',
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_books_title ON books(title);
CREATE INDEX idx_books_author ON books(author);
CREATE INDEX idx_books_genre ON books(genre);
CREATE INDEX idx_books_active ON books(active);

-- 3. Book Copies (Physical Inventory Units)
CREATE TABLE book_copies (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL REFERENCES books(id) ON DELETE RESTRICT,
    library_id BIGINT NOT NULL REFERENCES libraries(id) ON DELETE RESTRICT,
    barcode VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    acquisition_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_book_copy_status CHECK (
        status IN ('AVAILABLE', 'ON_LOAN', 'RESERVED', 'LOST', 'UNDER_MAINTENANCE', 'WITHDRAWN')
    )
);

CREATE INDEX idx_book_copies_book_id ON book_copies(book_id);
CREATE INDEX idx_book_copies_status ON book_copies(status);
CREATE INDEX idx_book_copies_library_id ON book_copies(library_id);

-- 4. Members (Borrowers)
CREATE TABLE members (
    id BIGSERIAL PRIMARY KEY,
    membership_number VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(50),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    membership_start_date DATE NOT NULL,
    membership_expiry_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_member_status CHECK (
        status IN ('ACTIVE', 'SUSPENDED', 'EXPIRED', 'INACTIVE')
    )
);

CREATE INDEX idx_members_status ON members(status);
CREATE INDEX idx_members_email ON members(email);

-- 5. User Accounts (Authentication & RBAC)
CREATE TABLE user_accounts (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    member_id BIGINT REFERENCES members(id) ON DELETE SET NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_user_role CHECK (
        role IN ('ADMIN', 'LIBRARIAN', 'MEMBER')
    )
);

CREATE INDEX idx_user_accounts_username ON user_accounts(username);
CREATE INDEX idx_user_accounts_member_id ON user_accounts(member_id);

-- 6. Loans (Circulation Transactions)
CREATE TABLE loans (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES members(id) ON DELETE RESTRICT,
    book_copy_id BIGINT NOT NULL REFERENCES book_copies(id) ON DELETE RESTRICT,
    issue_date TIMESTAMP WITH TIME ZONE NOT NULL,
    due_date TIMESTAMP WITH TIME ZONE NOT NULL,
    return_date TIMESTAMP WITH TIME ZONE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    loan_duration_days INT NOT NULL,
    daily_fine_rate_snapshot NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_loan_status CHECK (
        status IN ('ACTIVE', 'RETURNED', 'OVERDUE')
    )
);

CREATE INDEX idx_loans_member_id ON loans(member_id);
CREATE INDEX idx_loans_book_copy_id ON loans(book_copy_id);
CREATE INDEX idx_loans_status ON loans(status);
CREATE INDEX idx_loans_due_date ON loans(due_date);

-- 7. Reservations (FIFO Hold Queue)
CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL REFERENCES members(id) ON DELETE RESTRICT,
    book_id BIGINT NOT NULL REFERENCES books(id) ON DELETE RESTRICT,
    book_copy_id BIGINT REFERENCES book_copies(id) ON DELETE SET NULL,
    queue_position INT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    request_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    hold_expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_reservation_status CHECK (
        status IN ('PENDING', 'READY_FOR_PICKUP', 'FULFILLED', 'CANCELLED', 'EXPIRED')
    )
);

CREATE INDEX idx_reservations_member_id ON reservations(member_id);
CREATE INDEX idx_reservations_book_id ON reservations(book_id);
CREATE INDEX idx_reservations_status ON reservations(status);

-- 8. Fines & Penalty Management
CREATE TABLE fines (
    id BIGSERIAL PRIMARY KEY,
    loan_id BIGINT NOT NULL UNIQUE REFERENCES loans(id) ON DELETE RESTRICT,
    member_id BIGINT NOT NULL REFERENCES members(id) ON DELETE RESTRICT,
    fine_amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    amount_paid NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    amount_waived NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'UNPAID',
    accrual_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_fine_status CHECK (
        status IN ('UNPAID', 'PARTIALLY_PAID', 'PAID', 'WAIVED')
    ),
    CONSTRAINT chk_fine_amounts CHECK (
        fine_amount >= 0 AND amount_paid >= 0 AND amount_waived >= 0
    )
);

CREATE INDEX idx_fines_member_id ON fines(member_id);
CREATE INDEX idx_fines_status ON fines(status);

-- 9. Fine Transactions (Ledger for Payments and Authorized Waivers)
CREATE TABLE fine_transactions (
    id BIGSERIAL PRIMARY KEY,
    fine_id BIGINT NOT NULL REFERENCES fines(id) ON DELETE RESTRICT,
    transaction_type VARCHAR(30) NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    actor_username VARCHAR(100) NOT NULL,
    reason VARCHAR(500),
    transaction_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_fine_tx_type CHECK (
        transaction_type IN ('PAYMENT', 'WAIVER')
    )
);

CREATE INDEX idx_fine_tx_fine_id ON fine_transactions(fine_id);

-- 10. Refresh Tokens (JWT Session Management & Rotation)
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_account_id BIGINT NOT NULL REFERENCES user_accounts(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    replaced_by_token_hash VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_account_id);

-- 11. Audit Events (Tamper-Resistant Regulatory Activity Trail)
CREATE TABLE audit_events (
    id BIGSERIAL PRIMARY KEY,
    actor VARCHAR(100) NOT NULL,
    action_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100),
    correlation_id VARCHAR(100),
    ip_address VARCHAR(100),
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_events_actor ON audit_events(actor);
CREATE INDEX idx_audit_events_action ON audit_events(action_type);
CREATE INDEX idx_audit_events_created ON audit_events(created_at);

-- 12. Idempotency Records (Safe Retries on Mutating Lending Operations)
CREATE TABLE idempotency_records (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    request_hash VARCHAR(255) NOT NULL,
    response_status INT NOT NULL,
    response_body TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_idempotency_key ON idempotency_records(idempotency_key);
