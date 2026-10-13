-- ============================================================================
-- DIGITAL LIBRARY MANAGEMENT SYSTEM (DLMS)
-- SQL Analytics & Reporting Queries (Slide 11 Reference)
-- ============================================================================

-- Query 1: Category-wise Book Circulation and Fine Collection Analysis
-- Extracted through multi-table JOINs, GROUP BY, aggregations, and subqueries
SELECT 
    b.category AS book_category,
    COUNT(bi.issue_id) AS books_issued,
    COALESCE(SUM(f.amount), 0.0) AS fine_collected,
    ROUND(COUNT(bi.issue_id) * 100.0 / (SELECT COUNT(*) FROM book_issues), 1) AS share_of_issues
FROM books b
JOIN book_issues bi ON b.book_id = bi.book_id
LEFT JOIN fines f ON bi.issue_id = f.issue_id
GROUP BY b.category
ORDER BY books_issued DESC, fine_collected DESC;

-- Query 2: Active Borrowing by Member with Membership Limits Check
SELECT 
    m.member_id,
    m.full_name,
    m.membership_type,
    COUNT(bi.issue_id) AS active_loans,
    CASE m.membership_type
        WHEN 'STUDENT' THEN 3
        WHEN 'FACULTY' THEN 5
        WHEN 'GENERAL' THEN 2
        ELSE 0
    END AS max_limit,
    (CASE m.membership_type
        WHEN 'STUDENT' THEN 3
        WHEN 'FACULTY' THEN 5
        WHEN 'GENERAL' THEN 2
        ELSE 0
    END - COUNT(bi.issue_id)) AS remaining_allowance
FROM members m
LEFT JOIN book_issues bi ON m.member_id = bi.member_id AND bi.status = 'ISSUED'
GROUP BY m.member_id, m.full_name, m.membership_type
ORDER BY m.member_id;

-- Query 3: Overdue Book Circulation with Calculated Fines as of Current Date
SELECT 
    bi.issue_id,
    m.full_name AS member_name,
    m.membership_type,
    b.title AS book_title,
    bi.issue_date,
    bi.due_date,
    DATE('now') AS today_date,
    CAST((JULIANDAY('now') - JULIANDAY(bi.due_date)) AS INTEGER) AS days_overdue,
    (CAST((JULIANDAY('now') - JULIANDAY(bi.due_date)) AS INTEGER) * 5.00) AS pending_fine
FROM book_issues bi
JOIN members m ON bi.member_id = m.member_id
JOIN books b ON bi.book_id = b.book_id
WHERE bi.status = 'ISSUED' AND DATE('now') > bi.due_date;

-- Query 4: Total Revenue & Unpaid Fine Ledger
SELECT 
    COUNT(fine_id) AS total_fines_recorded,
    SUM(CASE WHEN paid_status = 'PAID' THEN amount ELSE 0 END) AS total_fines_collected,
    SUM(CASE WHEN paid_status = 'UNPAID' THEN amount ELSE 0 END) AS total_fines_outstanding,
    SUM(amount) AS total_fines_assessed
FROM fines;
