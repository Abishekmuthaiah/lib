package com.evoriea.dlms.data;

import com.evoriea.dlms.config.DBConnection;
import com.evoriea.dlms.dao.LibraryDAO;
import com.evoriea.dlms.model.Book;
import com.evoriea.dlms.model.Member;
import com.evoriea.dlms.model.MembershipType;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

/**
 * Seeds the database with the exact reference dataset from the Internship Presentation.
 * 
 * Includes:
 * - 12 Books across 5 categories (Slide 10 & 11)
 * - 6 Members spanning STUDENT, FACULTY, and GENERAL tiers
 * - 58 circulation issues reproducing the exact Slide 11 analytics:
 *     * Computer Science: 18 issues, Rs. 180.00 fines (31.0%)
 *     * Engineering Mathematics: 14 issues, Rs. 110.00 fines (24.1%)
 *     * Fiction: 11 issues, Rs. 45.00 fines (19.0%)
 *     * General Reference: 9 issues, Rs. 35.00 fines (15.5%)
 *     * Biography: 6 issues, Rs. 20.00 fines (10.3%)
 *     * Total: 58 issues, Rs. 390.00 fines
 * - Issue ID 7: Priya R returning "Effective Java" (ISBN 9780134685991) with 4 overdue days (Rs. 20.00 fine).
 */
public class DatabaseSeeder {

    private final LibraryDAO libraryDAO;

    public DatabaseSeeder(LibraryDAO libraryDAO) {
        this.libraryDAO = libraryDAO;
    }

    public boolean seedIfEmpty() throws SQLException {
        if (libraryDAO.getBookCount() == 0) {
            seedAll();
            return true;
        }
        return false;
    }

    public void seedAll() throws SQLException {
        Connection conn = DBConnection.getInstance().getConnection();
        boolean origAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);

        try {
            // Clear existing tables in reverse dependency order
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM fines;");
                stmt.execute("DELETE FROM book_issues;");
                stmt.execute("DELETE FROM members;");
                stmt.execute("DELETE FROM books;");
                if ("sqlite".equalsIgnoreCase(DBConnection.getInstance().getDbType())) {
                    stmt.execute("DELETE FROM sqlite_sequence WHERE name IN ('books', 'members', 'book_issues', 'fines');");
                }
            }

            // 1. Seed 12 Books across 5 categories (Slide 10 & 11)
            // Categories: Computer Science, Engineering Mathematics, Fiction, General Reference, Biography
            String insertBookSql = "INSERT INTO books (isbn, title, author, publisher, category, total_copies, available_copies) VALUES (?, ?, ?, ?, ?, ?, ?);";
            try (PreparedStatement pstmt = conn.prepareStatement(insertBookSql)) {
                // Book 1 (CS)
                addBookBatch(pstmt, "9780134685991", "Effective Java", "Joshua Bloch", "Addison-Wesley", "Computer Science", 5, 2);
                // Book 2 (CS)
                addBookBatch(pstmt, "9780132350884", "Clean Code", "Robert C. Martin", "Prentice Hall", "Computer Science", 4, 3);
                // Book 3 (CS)
                addBookBatch(pstmt, "9780262033848", "Introduction to Algorithms", "Thomas H. Cormen", "MIT Press", "Computer Science", 6, 4);
                // Book 4 (CS)
                addBookBatch(pstmt, "9780134757599", "Core Java Volume I", "Cay S. Horstmann", "Prentice Hall", "Computer Science", 4, 2);

                // Book 5 (Engineering Mathematics)
                addBookBatch(pstmt, "9780470458211", "Advanced Engineering Mathematics", "Erwin Kreyszig", "Wiley", "Engineering Mathematics", 5, 3);
                // Book 6 (Engineering Mathematics)
                addBookBatch(pstmt, "9780070146150", "Higher Engineering Mathematics", "B.S. Grewal", "Khanna Publishers", "Engineering Mathematics", 5, 2);

                // Book 7 (Fiction)
                addBookBatch(pstmt, "9780451524935", "1984", "George Orwell", "Signet Classic", "Fiction", 4, 2);
                // Book 8 (Fiction)
                addBookBatch(pstmt, "9780141439518", "Pride and Prejudice", "Jane Austen", "Penguin Classics", "Fiction", 3, 2);

                // Book 9 (General Reference)
                addBookBatch(pstmt, "9780199571123", "Oxford English Dictionary & Thesaurus", "Oxford Press", "Oxford", "General Reference", 3, 2);
                // Book 10 (General Reference)
                addBookBatch(pstmt, "9781465492456", "Science: The Definitive Visual Guide", "Adam Hart-Davis", "DK Publishing", "General Reference", 3, 2);

                // Book 11 (Biography)
                addBookBatch(pstmt, "9781451648539", "Steve Jobs", "Walter Isaacson", "Simon & Schuster", "Biography", 3, 2);
                // Book 12 (Biography)
                addBookBatch(pstmt, "9788173711466", "Wings of Fire", "A.P.J. Abdul Kalam", "Universities Press", "Biography", 4, 3);

                pstmt.executeBatch();
            }

            // 2. Seed 6 Members (Slide 10 mentions Priya R as Member 3, STUDENT)
            String insertMemberSql = "INSERT INTO members (full_name, email, phone, membership_type, join_date, status) VALUES (?, ?, ?, ?, ?, ?);";
            try (PreparedStatement pstmt = conn.prepareStatement(insertMemberSql)) {
                // Member 1
                addMemberBatch(pstmt, "Arun Kumar", "arun.kumar@psr.edu.in", "+91 9842100001", "STUDENT", LocalDate.of(2025, 8, 1), "ACTIVE");
                // Member 2
                addMemberBatch(pstmt, "Dr. K. Ramesh", "ramesh.k@psr.edu.in", "+91 9842100002", "FACULTY", LocalDate.of(2024, 6, 15), "ACTIVE");
                // Member 3 (Slide 10 Reference: Priya R, STUDENT)
                addMemberBatch(pstmt, "Priya R", "priya.r@psr.edu.in", "+91 9842100003", "STUDENT", LocalDate.of(2025, 8, 1), "ACTIVE");
                // Member 4
                addMemberBatch(pstmt, "Prof. M. Selvam", "selvam.m@psr.edu.in", "+91 9842100004", "FACULTY", LocalDate.of(2024, 1, 10), "ACTIVE");
                // Member 5
                addMemberBatch(pstmt, "Suresh V", "suresh.v@gmail.com", "+91 9842100005", "GENERAL", LocalDate.of(2026, 1, 5), "ACTIVE");
                // Member 6
                addMemberBatch(pstmt, "Divya M", "divya.m@psr.edu.in", "+91 9842100006", "STUDENT", LocalDate.of(2025, 9, 12), "ACTIVE");

                pstmt.executeBatch();
            }

            // 3. Seed historical book issues and fines matching Slide 10 & 11
            // Distribution target:
            // - Computer Science (Books 1..4): 18 issues, total fines = Rs. 180.00
            // - Engineering Mathematics (Books 5..6): 14 issues, total fines = Rs. 110.00
            // - Fiction (Books 7..8): 11 issues, total fines = Rs. 45.00
            // - General Reference (Books 9..10): 9 issues, total fines = Rs. 35.00
            // - Biography (Books 11..12): 6 issues, total fines = Rs. 20.00
            // Total: 58 issues, Rs. 390.00 fines!

            String insertIssueSql = "INSERT INTO book_issues (issue_id, book_id, member_id, issue_date, due_date, return_date, status) VALUES (?, ?, ?, ?, ?, ?, ?);";
            String insertFineSql = "INSERT INTO fines (issue_id, member_id, amount, reason, paid_status, created_at) VALUES (?, ?, ?, ?, ?, ?);";

            PreparedStatement pstmtIssue = conn.prepareStatement(insertIssueSql);
            PreparedStatement pstmtFine = conn.prepareStatement(insertFineSql);

            int currentIssueId = 1;

            // --- CATEGORY 1: Computer Science (Books 1-4, 18 issues, Rs. 180 fines) ---
            // Issue 7 is specifically reserved for Priya R (Member 3) with Book 1 ("Effective Java")
            // Overdue fine: 4 days * Rs. 5 = Rs. 20.00 UNPAID
            // Other fines to reach Rs. 180 total: Rs. 180 - Rs. 20 = Rs. 160 across CS issues.

            // 18 issues for CS:
            int[] csBooks = {1, 2, 3, 4};
            int[] csMembers = {1, 2, 3, 4, 6};

            for (int i = 0; i < 18; i++) {
                int issueId = currentIssueId++;
                int bookId = csBooks[i % csBooks.length];
                int memberId = csMembers[i % csMembers.length];
                LocalDate issueDate = LocalDate.of(2026, 4, 1).plusDays(i * 3);
                LocalDate dueDate = issueDate.plusDays(14);
                LocalDate returnDate = dueDate.plusDays(0);
                String status = "RETURNED";
                double fineAmt = 0;
                String paidStatus = "PAID";

                if (issueId == 7) {
                    // Exact match for Slide 10: Issue ID 7 | Priya R (STUDENT) | Effective Java
                    bookId = 1; // Effective Java
                    memberId = 3; // Priya R
                    issueDate = LocalDate.of(2026, 5, 27);
                    dueDate = LocalDate.of(2026, 6, 10);
                    returnDate = LocalDate.of(2026, 6, 14);
                    fineAmt = 20.00; // 4 days overdue
                    paidStatus = "UNPAID";
                } else if (i == 0) { // issue 1: 8 days overdue = Rs. 40
                    returnDate = dueDate.plusDays(8);
                    fineAmt = 40.00;
                } else if (i == 1) { // issue 2: 6 days overdue = Rs. 30
                    returnDate = dueDate.plusDays(6);
                    fineAmt = 30.00;
                } else if (i == 2) { // issue 3: 10 days overdue = Rs. 50
                    returnDate = dueDate.plusDays(10);
                    fineAmt = 50.00;
                } else if (i == 3) { // issue 4: 8 days overdue = Rs. 40
                    returnDate = dueDate.plusDays(8);
                    fineAmt = 40.00;
                } else if (i >= 16) {
                    // Two active loans for Priya R (Member 3) to match Slide 10 check:
                    // "[CHECK] Member 3 (Priya R, STUDENT) active loans = 2 / limit 3 -> OK"
                    status = "ISSUED";
                    returnDate = null;
                    memberId = 3;
                }

                pstmtIssue.setInt(1, issueId);
                pstmtIssue.setInt(2, bookId);
                pstmtIssue.setInt(3, memberId);
                pstmtIssue.setDate(4, Date.valueOf(issueDate));
                pstmtIssue.setDate(5, Date.valueOf(dueDate));
                if (returnDate != null) {
                    pstmtIssue.setDate(6, Date.valueOf(returnDate));
                } else {
                    pstmtIssue.setNull(6, java.sql.Types.DATE);
                }
                pstmtIssue.setString(7, status);
                pstmtIssue.executeUpdate();

                if (fineAmt > 0) {
                    pstmtFine.setInt(1, issueId);
                    pstmtFine.setInt(2, memberId);
                    pstmtFine.setDouble(3, fineAmt);
                    long days = (long) (fineAmt / 5.0);
                    pstmtFine.setString(4, String.format("Overdue return: %d days past due date %s (Rs. 5.00/day)", days, dueDate));
                    pstmtFine.setString(5, paidStatus);
                    pstmtFine.setDate(6, Date.valueOf(returnDate != null ? returnDate : dueDate));
                    pstmtFine.executeUpdate();
                }
            }

            // --- CATEGORY 2: Engineering Mathematics (Books 5-6, 14 issues, Rs. 110 fines) ---
            int[] mathBooks = {5, 6};
            for (int i = 0; i < 14; i++) {
                int issueId = currentIssueId++;
                int bookId = mathBooks[i % mathBooks.length];
                int memberId = (i % 5) + 1;
                LocalDate issueDate = LocalDate.of(2026, 4, 5).plusDays(i * 3);
                LocalDate dueDate = issueDate.plusDays(memberId == 2 || memberId == 4 ? 30 : 14);
                LocalDate returnDate = dueDate;
                String status = "RETURNED";
                double fineAmt = 0;

                // Fines distribution summing to 110: 50 + 35 + 25 = 110
                if (i == 0) {
                    returnDate = dueDate.plusDays(10);
                    fineAmt = 50.00;
                } else if (i == 1) {
                    returnDate = dueDate.plusDays(7);
                    fineAmt = 35.00;
                } else if (i == 2) {
                    returnDate = dueDate.plusDays(5);
                    fineAmt = 25.00;
                }

                pstmtIssue.setInt(1, issueId);
                pstmtIssue.setInt(2, bookId);
                pstmtIssue.setInt(3, memberId);
                pstmtIssue.setDate(4, Date.valueOf(issueDate));
                pstmtIssue.setDate(5, Date.valueOf(dueDate));
                pstmtIssue.setDate(6, Date.valueOf(returnDate));
                pstmtIssue.setString(7, status);
                pstmtIssue.executeUpdate();

                if (fineAmt > 0) {
                    pstmtFine.setInt(1, issueId);
                    pstmtFine.setInt(2, memberId);
                    pstmtFine.setDouble(3, fineAmt);
                    long days = (long) (fineAmt / 5.0);
                    pstmtFine.setString(4, String.format("Overdue return: %d days past due date %s (Rs. 5.00/day)", days, dueDate));
                    pstmtFine.setString(5, "PAID");
                    pstmtFine.setDate(6, Date.valueOf(returnDate));
                    pstmtFine.executeUpdate();
                }
            }

            // --- CATEGORY 3: Fiction (Books 7-8, 11 issues, Rs. 45 fines) ---
            int[] ficBooks = {7, 8};
            for (int i = 0; i < 11; i++) {
                int issueId = currentIssueId++;
                int bookId = ficBooks[i % ficBooks.length];
                int memberId = (i % 6) + 1;
                LocalDate issueDate = LocalDate.of(2026, 4, 10).plusDays(i * 4);
                LocalDate dueDate = issueDate.plusDays(14);
                LocalDate returnDate = dueDate;
                String status = "RETURNED";
                double fineAmt = 0;

                // Fines distribution summing to 45: 25 + 20 = 45
                if (i == 0) {
                    returnDate = dueDate.plusDays(5);
                    fineAmt = 25.00;
                } else if (i == 1) {
                    returnDate = dueDate.plusDays(4);
                    fineAmt = 20.00;
                }

                pstmtIssue.setInt(1, issueId);
                pstmtIssue.setInt(2, bookId);
                pstmtIssue.setInt(3, memberId);
                pstmtIssue.setDate(4, Date.valueOf(issueDate));
                pstmtIssue.setDate(5, Date.valueOf(dueDate));
                pstmtIssue.setDate(6, Date.valueOf(returnDate));
                pstmtIssue.setString(7, status);
                pstmtIssue.executeUpdate();

                if (fineAmt > 0) {
                    pstmtFine.setInt(1, issueId);
                    pstmtFine.setInt(2, memberId);
                    pstmtFine.setDouble(3, fineAmt);
                    long days = (long) (fineAmt / 5.0);
                    pstmtFine.setString(4, String.format("Overdue return: %d days past due date %s (Rs. 5.00/day)", days, dueDate));
                    pstmtFine.setString(5, "PAID");
                    pstmtFine.setDate(6, Date.valueOf(returnDate));
                    pstmtFine.executeUpdate();
                }
            }

            // --- CATEGORY 4: General Reference (Books 9-10, 9 issues, Rs. 35 fines) ---
            int[] refBooks = {9, 10};
            for (int i = 0; i < 9; i++) {
                int issueId = currentIssueId++;
                int bookId = refBooks[i % refBooks.length];
                int memberId = (i % 6) + 1;
                LocalDate issueDate = LocalDate.of(2026, 4, 12).plusDays(i * 4);
                LocalDate dueDate = issueDate.plusDays(7);
                LocalDate returnDate = dueDate;
                String status = "RETURNED";
                double fineAmt = 0;

                // Fines distribution summing to 35: 20 + 15 = 35
                if (i == 0) {
                    returnDate = dueDate.plusDays(4);
                    fineAmt = 20.00;
                } else if (i == 1) {
                    returnDate = dueDate.plusDays(3);
                    fineAmt = 15.00;
                }

                pstmtIssue.setInt(1, issueId);
                pstmtIssue.setInt(2, bookId);
                pstmtIssue.setInt(3, memberId);
                pstmtIssue.setDate(4, Date.valueOf(issueDate));
                pstmtIssue.setDate(5, Date.valueOf(dueDate));
                pstmtIssue.setDate(6, Date.valueOf(returnDate));
                pstmtIssue.setString(7, status);
                pstmtIssue.executeUpdate();

                if (fineAmt > 0) {
                    pstmtFine.setInt(1, issueId);
                    pstmtFine.setInt(2, memberId);
                    pstmtFine.setDouble(3, fineAmt);
                    long days = (long) (fineAmt / 5.0);
                    pstmtFine.setString(4, String.format("Overdue return: %d days past due date %s (Rs. 5.00/day)", days, dueDate));
                    pstmtFine.setString(5, "PAID");
                    pstmtFine.setDate(6, Date.valueOf(returnDate));
                    pstmtFine.executeUpdate();
                }
            }

            // --- CATEGORY 5: Biography (Books 11-12, 6 issues, Rs. 20 fines) ---
            int[] bioBooks = {11, 12};
            for (int i = 0; i < 6; i++) {
                int issueId = currentIssueId++;
                int bookId = bioBooks[i % bioBooks.length];
                int memberId = (i % 6) + 1;
                LocalDate issueDate = LocalDate.of(2026, 4, 15).plusDays(i * 5);
                LocalDate dueDate = issueDate.plusDays(14);
                LocalDate returnDate = dueDate;
                String status = "RETURNED";
                double fineAmt = 0;

                // Fines distribution summing to 20: 20
                if (i == 0) {
                    returnDate = dueDate.plusDays(4);
                    fineAmt = 20.00;
                }

                pstmtIssue.setInt(1, issueId);
                pstmtIssue.setInt(2, bookId);
                pstmtIssue.setInt(3, memberId);
                pstmtIssue.setDate(4, Date.valueOf(issueDate));
                pstmtIssue.setDate(5, Date.valueOf(dueDate));
                pstmtIssue.setDate(6, Date.valueOf(returnDate));
                pstmtIssue.setString(7, status);
                pstmtIssue.executeUpdate();

                if (fineAmt > 0) {
                    pstmtFine.setInt(1, issueId);
                    pstmtFine.setInt(2, memberId);
                    pstmtFine.setDouble(3, fineAmt);
                    long days = (long) (fineAmt / 5.0);
                    pstmtFine.setString(4, String.format("Overdue return: %d days past due date %s (Rs. 5.00/day)", days, dueDate));
                    pstmtFine.setString(5, "PAID");
                    pstmtFine.setDate(6, Date.valueOf(returnDate));
                    pstmtFine.executeUpdate();
                }
            }

            pstmtIssue.close();
            pstmtFine.close();

            conn.commit();

        } catch (Exception e) {
            conn.rollback();
            throw new SQLException("Failed to seed reference dataset: " + e.getMessage(), e);
        } finally {
            conn.setAutoCommit(origAutoCommit);
        }
    }

    private void addBookBatch(PreparedStatement pstmt, String isbn, String title, String author, String publisher, String category, int total, int avail) throws SQLException {
        pstmt.setString(1, isbn);
        pstmt.setString(2, title);
        pstmt.setString(3, author);
        pstmt.setString(4, publisher);
        pstmt.setString(5, category);
        pstmt.setInt(6, total);
        pstmt.setInt(7, avail);
        pstmt.addBatch();
    }

    private void addMemberBatch(PreparedStatement pstmt, String name, String email, String phone, String type, LocalDate joinDate, String status) throws SQLException {
        pstmt.setString(1, name);
        pstmt.setString(2, email);
        pstmt.setString(3, phone);
        pstmt.setString(4, type);
        pstmt.setDate(5, Date.valueOf(joinDate));
        pstmt.setString(6, status);
        pstmt.addBatch();
    }
}
