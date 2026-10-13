-- ============================================================================
-- DIGITAL LIBRARY MANAGEMENT SYSTEM (DLMS)
-- Relational Database Schema (3NF Normalized)
-- Evoriea Infotech Pvt. Ltd. | Internship Project
-- Candidate: Abishek Muthaiah S K (Reg No: 95192301003)
-- College: P.S.R. Engineering College, Sivakasi
-- ============================================================================

-- Enable Foreign Key Support (for SQLite)
PRAGMA foreign_keys = ON;

-- ----------------------------------------------------------------------------
-- Table 1: BOOKS (Entity: Books Catalogue)
-- Normalized to 3NF: No repeating groups, full functional dependency on PK.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS books (
    book_id INTEGER PRIMARY KEY AUTOINCREMENT,
    isbn VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(150) NOT NULL,
    publisher VARCHAR(150) NOT NULL,
    category VARCHAR(100) NOT NULL,
    total_copies INTEGER NOT NULL CHECK (total_copies >= 0),
    available_copies INTEGER NOT NULL CHECK (available_copies >= 0 AND available_copies <= total_copies)
);

-- ----------------------------------------------------------------------------
-- Table 2: MEMBERS (Entity: Library Members Registry)
-- Normalized to 3NF: Membership rules determine loan limits & durations.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS members (
    member_id INTEGER PRIMARY KEY AUTOINCREMENT,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    membership_type VARCHAR(20) NOT NULL CHECK (membership_type IN ('STUDENT', 'FACULTY', 'GENERAL')),
    join_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'EXPIRED'))
);

-- ----------------------------------------------------------------------------
-- Table 3: BOOK_ISSUES (Entity: Circulation Transactions)
-- Atomic issue/return transactions linking Books and Members.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS book_issues (
    issue_id INTEGER PRIMARY KEY AUTOINCREMENT,
    book_id INTEGER NOT NULL,
    member_id INTEGER NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    return_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ISSUED' CHECK (status IN ('ISSUED', 'RETURNED')),
    FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE RESTRICT,
    FOREIGN KEY (member_id) REFERENCES members(member_id) ON DELETE RESTRICT
);

-- ----------------------------------------------------------------------------
-- Table 4: FINES (Entity: Overdue Fine Ledger & Audit Trail)
-- Triggered/Calculated on return when return_date > due_date (Rs. 5.00/day).
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS fines (
    fine_id INTEGER PRIMARY KEY AUTOINCREMENT,
    issue_id INTEGER NOT NULL,
    member_id INTEGER NOT NULL,
    amount DECIMAL(10, 2) NOT NULL CHECK (amount >= 0),
    reason VARCHAR(200) NOT NULL,
    paid_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID' CHECK (paid_status IN ('UNPAID', 'PAID')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (issue_id) REFERENCES book_issues(issue_id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES members(member_id) ON DELETE RESTRICT
);

-- ----------------------------------------------------------------------------
-- B-Tree Performance Indexes for Fast Foreign Key Joins & Search Lookups
-- ----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_books_isbn ON books(isbn);
CREATE INDEX IF NOT EXISTS idx_books_category ON books(category);
CREATE INDEX IF NOT EXISTS idx_books_title ON books(title);

CREATE INDEX IF NOT EXISTS idx_members_email ON members(email);
CREATE INDEX IF NOT EXISTS idx_members_type ON members(membership_type);

CREATE INDEX IF NOT EXISTS idx_issues_book_id ON book_issues(book_id);
CREATE INDEX IF NOT EXISTS idx_issues_member_id ON book_issues(member_id);
CREATE INDEX IF NOT EXISTS idx_issues_status ON book_issues(status);
CREATE INDEX IF NOT EXISTS idx_issues_due_date ON book_issues(due_date);

CREATE INDEX IF NOT EXISTS idx_fines_issue_id ON fines(issue_id);
CREATE INDEX IF NOT EXISTS idx_fines_member_id ON fines(member_id);
CREATE INDEX IF NOT EXISTS idx_fines_paid_status ON fines(paid_status);
