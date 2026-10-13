package com.evoriea.dlms.test;

import com.evoriea.dlms.config.DBConnection;
import com.evoriea.dlms.dao.LibraryDAO;
import com.evoriea.dlms.data.DatabaseSeeder;
import com.evoriea.dlms.model.Book;
import com.evoriea.dlms.model.BookIssue;
import com.evoriea.dlms.model.CategoryAnalytics;
import com.evoriea.dlms.model.Fine;
import com.evoriea.dlms.model.Member;
import com.evoriea.dlms.model.MembershipType;
import com.evoriea.dlms.service.LibraryService;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Automated Verification and Test Suite for DLMS.
 * Tests ACID transactions, singleton connectivity, business rules,
 * borrowing quotas, fine calculations, and Slide 11 analytics integrity.
 */
public class DLMSTestSuite {

    private final LibraryDAO dao = new LibraryDAO();
    private final LibraryService service = new LibraryService(dao);

    private int passed = 0;
    private int failed = 0;

    public static void main(String[] args) {
        DLMSTestSuite suite = new DLMSTestSuite();
        suite.runAllTests();
    }

    public void runAllTests() {
        System.out.println("==========================================================================");
        System.out.println("           DLMS AUTOMATED TEST & VERIFICATION SUITE");
        System.out.println("==========================================================================");

        testSingletonDBConnection();
        testSchemaAndSeeding();
        testMembershipPolicies();
        testStockDecrementOnIssue();
        testBorrowingQuotaEnforcement();
        testFineCalculationAndReturnStockRestoration();
        testSlide11CategoryAnalyticsIntegrity();

        System.out.println("==========================================================================");
        System.out.println(String.format("TEST RESULTS: %d PASSED, %d FAILED (TOTAL %d)", passed, failed, (passed + failed)));
        System.out.println("==========================================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private void assertTrue(String testName, boolean condition, String errorMsg) {
        if (condition) {
            System.out.println("  [PASS] " + testName);
            passed++;
        } else {
            System.err.println("  [FAIL] " + testName + " -> " + errorMsg);
            failed++;
        }
    }

    private void testSingletonDBConnection() {
        try {
            DBConnection db1 = DBConnection.getInstance();
            DBConnection db2 = DBConnection.getInstance();
            Connection conn = db1.getConnection();
            assertTrue("Singleton Connection Pattern", db1 == db2 && conn != null && !conn.isClosed(), "Singleton instances do not match or connection is closed");
        } catch (Exception e) {
            assertTrue("Singleton Connection Pattern", false, e.getMessage());
        }
    }

    private void testSchemaAndSeeding() {
        try {
            dao.initializeSchema();
            DatabaseSeeder seeder = new DatabaseSeeder(dao);
            seeder.seedAll();

            int books = dao.getBookCount();
            int members = dao.getMemberCount();
            int issues = dao.getIssueCount();

            assertTrue("Schema Initialization & Reference Seeding",
                    books == 12 && members == 6 && issues == 58,
                    String.format("Expected 12 books, 6 members, 58 issues. Got: %d books, %d members, %d issues", books, members, issues));
        } catch (Exception e) {
            assertTrue("Schema Initialization & Reference Seeding", false, e.getMessage());
        }
    }

    private void testMembershipPolicies() {
        assertTrue("Student Tier: 14 days, Limit 3",
                MembershipType.STUDENT.getLoanPeriodDays() == 14 && MembershipType.STUDENT.getBorrowingLimit() == 3,
                "Student policy mismatch");

        assertTrue("Faculty Tier: 30 days, Limit 5",
                MembershipType.FACULTY.getLoanPeriodDays() == 30 && MembershipType.FACULTY.getBorrowingLimit() == 5,
                "Faculty policy mismatch");

        assertTrue("General Tier: 7 days, Limit 2",
                MembershipType.GENERAL.getLoanPeriodDays() == 7 && MembershipType.GENERAL.getBorrowingLimit() == 2,
                "General policy mismatch");
    }

    private void testStockDecrementOnIssue() {
        try {
            // Member 1 (Arun Kumar) has 0 active loans currently
            Book bookBefore = dao.getBookById(2); // Clean Code
            int initialAvail = bookBefore.getAvailableCopies();

            BookIssue issue = dao.issueBook(1, 2, LocalDate.now());
            Book bookAfter = dao.getBookById(2);

            assertTrue("Atomic Inventory Stock Decrement",
                    bookAfter.getAvailableCopies() == initialAvail - 1 && "ISSUED".equalsIgnoreCase(issue.getStatus()),
                    String.format("Stock not decremented. Before: %d, After: %d", initialAvail, bookAfter.getAvailableCopies()));

            // Return it so tests remain clean
            dao.returnBook(issue.getIssueId(), LocalDate.now());
        } catch (Exception e) {
            assertTrue("Atomic Inventory Stock Decrement", false, e.getMessage());
        }
    }

    private void testBorrowingQuotaEnforcement() {
        try {
            // Member 3 (Priya R) already has 2 active loans seeded. Limit is 3.
            int loans = dao.getActiveLoanCount(3);
            assertTrue("Priya R Active Loans Count Check", loans == 2, "Expected 2 active loans for Priya R, found: " + loans);

            // Borrow 3rd book -> OK
            BookIssue thirdLoan = dao.issueBook(3, 4, LocalDate.now()); // Core Java

            // Attempt 4th book -> Must be rejected by business rules
            boolean caughtLimitException = false;
            try {
                service.issueBook(3, "1", LocalDate.now());
            } catch (IllegalStateException e) {
                caughtLimitException = true;
            }

            assertTrue("Borrowing Quota Limit Enforcement (Rejects > 3 for Student)",
                    caughtLimitException,
                    "Expected IllegalStateException when exceeding quota limit");

            // Revert 3rd loan to maintain seed state
            dao.returnBook(thirdLoan.getIssueId(), LocalDate.now());
        } catch (Exception e) {
            assertTrue("Borrowing Quota Limit Enforcement", false, e.getMessage());
        }
    }

    private void testFineCalculationAndReturnStockRestoration() {
        try {
            // Issue book with fixed past date to simulate overdue
            LocalDate pastIssueDate = LocalDate.of(2026, 5, 1);
            LocalDate returnDate = LocalDate.of(2026, 5, 19); // 18 days later. Student loan is 14 days. 4 days overdue!
            
            Book bookBefore = dao.getBookById(3); // Algorithms
            int stockBefore = bookBefore.getAvailableCopies();

            BookIssue issue = dao.issueBook(1, 3, pastIssueDate);

            // Return on 2026-05-19: 4 days overdue -> 4 * 5.00 = Rs. 20.00
            Fine fine = dao.returnBook(issue.getIssueId(), returnDate);
            Book bookAfter = dao.getBookById(3);

            boolean stockRestored = bookAfter.getAvailableCopies() == stockBefore;
            boolean fineCorrect = fine != null && fine.getAmount() == 20.00 && fine.getOverdueDays() == 4;

            assertTrue("Overdue Fine Computation (4 days x Rs. 5 = Rs. 20) & Stock Restoration",
                    stockRestored && fineCorrect,
                    String.format("Fine result: %s, Stock before: %d, Stock after: %d", fine, stockBefore, bookAfter.getAvailableCopies()));
        } catch (Exception e) {
            assertTrue("Overdue Fine Computation", false, e.getMessage());
        }
    }

    private void testSlide11CategoryAnalyticsIntegrity() {
        try {
            // Reseed clean state
            DatabaseSeeder seeder = new DatabaseSeeder(dao);
            seeder.seedAll();

            List<CategoryAnalytics> analytics = dao.getCategoryAnalytics();
            int totalIssues = 0;
            double totalFines = 0;
            CategoryAnalytics csAnalytics = null;

            for (CategoryAnalytics ca : analytics) {
                totalIssues += ca.getBooksIssued();
                totalFines += ca.getFineCollected();
                if ("Computer Science".equalsIgnoreCase(ca.getCategory())) {
                    csAnalytics = ca;
                }
            }

            boolean issuesMatch = totalIssues == 58;
            boolean finesMatch = Math.abs(totalFines - 390.00) < 0.01;
            boolean csMatch = csAnalytics != null && csAnalytics.getBooksIssued() == 18 && Math.abs(csAnalytics.getFineCollected() - 180.00) < 0.01;

            assertTrue("Slide 11 Analytics Integrity: 58 Issues, Rs. 390.00 Total Fines, CS = 18 issues / Rs. 180 fine",
                    issuesMatch && finesMatch && csMatch,
                    String.format("Expected 58 issues, Rs. 390 fine. Got: %d issues, Rs. %.2f fine", totalIssues, totalFines));
        } catch (Exception e) {
            assertTrue("Slide 11 Analytics Integrity", false, e.getMessage());
        }
    }
}
