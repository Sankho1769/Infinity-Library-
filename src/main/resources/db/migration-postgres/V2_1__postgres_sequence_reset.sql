-- ============================================================================
-- PostgreSQL-Specific Sequence Alignment for Seed Data
-- Schema Version: 2.1 (Flyway V2_1)
-- ============================================================================

SELECT setval('libraries_id_seq', (SELECT MAX(id) FROM libraries));
SELECT setval('books_id_seq', (SELECT MAX(id) FROM books));
SELECT setval('book_copies_id_seq', (SELECT MAX(id) FROM book_copies));
SELECT setval('members_id_seq', (SELECT MAX(id) FROM members));
SELECT setval('user_accounts_id_seq', (SELECT MAX(id) FROM user_accounts));
