-- ============================================================================
-- Infinity Library Management System - Initial Seed Data
-- Schema Version: 2.0 (Flyway V2)
-- Default administrative users, demo catalog, and sample members
-- ============================================================================

-- 1. Default Library Instance
INSERT INTO libraries (
    name, code, address, email, phone, timezone,
    loan_duration_days, max_loans_per_member, daily_fine_rate, max_fine_per_loan, reservation_hold_hours, active
) VALUES (
    'Infinity Central Academic Library',
    'INF-CENTRAL-01',
    '742 Academic Boulevard, Knowledge District, Tech Campus',
    'contact@infinitylibrary.org',
    '+1 (555) 019-2831',
    'UTC',
    14,
    5,
    5.00,
    500.00,
    48,
    TRUE
);

-- 2. Initial Sample Members
INSERT INTO members (
    membership_number, name, email, phone, status,
    membership_start_date, membership_expiry_date
) VALUES (
    'MEM-2026-0001',
    'Dr. Elena Rostova',
    'elena.rostova@university.edu',
    '+1-555-0101',
    'ACTIVE',
    CURRENT_DATE - INTERVAL '30' DAY,
    CURRENT_DATE + INTERVAL '335' DAY
), (
    'MEM-2026-0002',
    'Marcus Vance',
    'marcus.vance@university.edu',
    '+1-555-0102',
    'ACTIVE',
    CURRENT_DATE - INTERVAL '15' DAY,
    CURRENT_DATE + INTERVAL '350' DAY
), (
    'MEM-2026-0003',
    'Aaliyah Khan',
    'aaliyah.khan@university.edu',
    '+1-555-0103',
    'SUSPENDED',
    CURRENT_DATE - INTERVAL '60' DAY,
    CURRENT_DATE + INTERVAL '305' DAY
);

-- 3. User Accounts
INSERT INTO user_accounts (
    username, password_hash, role, member_id, active
) VALUES (
    'admin',
    '$argon2id$v=19$m=65536,t=3,p=1$0uMhQ6gT8Xl9eQvB9R7tYw$l2u3F+4H8Y9Wk0J1L2M3N4O5P6Q7R8S9T0U1V2W3X4Y',
    'ADMIN',
    NULL,
    TRUE
), (
    'librarian',
    '$argon2id$v=19$m=65536,t=3,p=1$0uMhQ6gT8Xl9eQvB9R7tYw$l2u3F+4H8Y9Wk0J1L2M3N4O5P6Q7R8S9T0U1V2W3X4Y',
    'LIBRARIAN',
    NULL,
    TRUE
), (
    'member_elena',
    '$argon2id$v=19$m=65536,t=3,p=1$0uMhQ6gT8Xl9eQvB9R7tYw$l2u3F+4H8Y9Wk0J1L2M3N4O5P6Q7R8S9T0U1V2W3X4Y',
    'MEMBER',
    1,
    TRUE
);

-- 4. Initial Books (Bibliographic Titles)
INSERT INTO books (
    title, isbn, author, publisher, publication_year, genre, language, description, active
) VALUES (
    'Designing Data-Intensive Applications',
    '978-1449373320',
    'Martin Kleppmann',
    'O Reilly Media',
    2017,
    'Distributed Systems',
    'English',
    'The big ideas behind reliable, scalable, and maintainable systems.',
    TRUE
), (
    'Clean Architecture: A Craftsman Guide',
    '978-0134494166',
    'Robert C. Martin',
    'Prentice Hall',
    2017,
    'Software Architecture',
    'English',
    'A guide to building maintainable, testable, and robust enterprise software.',
    TRUE
), (
    'Concurrency in Practice',
    '978-0321349606',
    'Brian Goetz',
    'Addison-Wesley',
    2006,
    'Java Engineering',
    'English',
    'The definitive guide to multithreading and concurrency in Java.',
    TRUE
);

-- 5. Physical Book Copies
INSERT INTO book_copies (
    book_id, library_id, barcode, status, acquisition_date
) VALUES (
    1, 1, 'DDIA-CPY-001', 'AVAILABLE', '2025-01-10'
), (
    1, 1, 'DDIA-CPY-002', 'AVAILABLE', '2025-01-10'
), (
    2, 1, 'CA-CPY-001', 'AVAILABLE', '2025-02-15'
), (
    2, 1, 'CA-CPY-002', 'AVAILABLE', '2025-02-15'
), (
    3, 1, 'CIP-CPY-001', 'AVAILABLE', '2025-03-01'
);
