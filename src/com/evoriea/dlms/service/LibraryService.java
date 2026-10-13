package com.evoriea.dlms.service;

import com.evoriea.dlms.dao.LibraryDAO;
import com.evoriea.dlms.model.Book;
import com.evoriea.dlms.model.BookIssue;
import com.evoriea.dlms.model.CategoryAnalytics;
import com.evoriea.dlms.model.Fine;
import com.evoriea.dlms.model.Member;
import com.evoriea.dlms.model.MembershipType;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service Layer enforcing core business logic, membership constraints,
 * loan periods, borrowing quotas, and the overdue fine calculation engine.
 * 
 * Architectural Layer 2: Service / Business Logic Layer (Slide 6 & 8)
 */
public class LibraryService {

    private final LibraryDAO libraryDAO;

    public LibraryService() {
        this.libraryDAO = new LibraryDAO();
    }

    public LibraryService(LibraryDAO libraryDAO) {
        this.libraryDAO = libraryDAO;
    }

    public LibraryDAO getLibraryDAO() {
        return libraryDAO;
    }

    /**
     * Issues a book to a member, enforcing all business constraints:
     * - Member must exist and be ACTIVE
     * - Member cannot exceed their tier borrowing limit (STUDENT: 3, FACULTY: 5, GENERAL: 2)
     * - Book must exist and have available copies > 0
     * - Automatically calculates due date based on membership loan duration:
     *   STUDENT = 14 days, FACULTY = 30 days, GENERAL = 7 days
     */
    public BookIssue issueBook(int memberId, String isbnOrBookId, LocalDate issueDate) throws SQLException {
        if (issueDate == null) {
            issueDate = LocalDate.now();
        }

        // Validate member
        Member member = libraryDAO.getMemberById(memberId);
        if (member == null) {
            throw new IllegalArgumentException("Validation Error: Member ID #" + memberId + " not found.");
        }
        if (!"ACTIVE".equalsIgnoreCase(member.getStatus())) {
            throw new IllegalStateException("Validation Error: Member status is '" + member.getStatus() + "'. Inactive members cannot borrow books.");
        }

        // Check active loan limit
        int currentLoans = libraryDAO.getActiveLoanCount(memberId);
        int maxLimit = member.getMembershipType().getBorrowingLimit();
        if (currentLoans >= maxLimit) {
            throw new IllegalStateException(String.format(
                "Borrowing Quota Exceeded: %s (%s) currently has %d active loans. Maximum limit is %d books.",
                member.getFullName(), member.getMembershipType(), currentLoans, maxLimit));
        }

        // Resolve Book by ID or ISBN
        Book book = null;
        try {
            int parsedId = Integer.parseInt(isbnOrBookId.trim());
            book = libraryDAO.getBookById(parsedId);
        } catch (NumberFormatException ignored) {
            // Not a numeric ID, search by ISBN
        }
        if (book == null) {
            book = libraryDAO.getBookByIsbn(isbnOrBookId.trim());
        }
        if (book == null) {
            throw new IllegalArgumentException("Validation Error: Book not found with ID or ISBN '" + isbnOrBookId + "'.");
        }

        if (book.getAvailableCopies() <= 0) {
            throw new IllegalStateException("Inventory Error: Book '" + book.getTitle() + "' has 0 copies available for borrowing.");
        }

        // Delegate to transactional DAO execution
        return libraryDAO.issueBook(memberId, book.getBookId(), issueDate);
    }

    /**
     * Processes a book return and calculates overdue fines.
     * Formula:
     *   Overdue Days = Return Date - Due Date (if positive)
     *   Fine = Overdue Days * Rs. 5.00/day
     */
    public Fine returnBook(int issueId, LocalDate returnDate) throws SQLException {
        if (returnDate == null) {
            returnDate = LocalDate.now();
        }

        BookIssue issue = libraryDAO.getIssueById(issueId);
        if (issue == null) {
            throw new IllegalArgumentException("Validation Error: Circulation Issue #" + issueId + " does not exist.");
        }
        if ("RETURNED".equalsIgnoreCase(issue.getStatus())) {
            throw new IllegalStateException("Validation Error: Book for Issue #" + issueId + " has already been returned on " + issue.getReturnDate() + ".");
        }

        return libraryDAO.returnBook(issueId, returnDate);
    }

    /**
     * Calculates pending fine for an active issue without completing return.
     */
    public double estimatePendingFine(LocalDate dueDate, LocalDate checkDate) {
        if (dueDate == null || checkDate == null || !checkDate.isAfter(dueDate)) {
            return 0.0;
        }
        long overdueDays = ChronoUnit.DAYS.between(dueDate, checkDate);
        return overdueDays * Fine.FINE_RATE_PER_DAY;
    }

    /**
     * Retrieves fine details with formatted receipt string.
     */
    public String getFormattedFineReceipt(int issueId) throws SQLException {
        Fine fine = libraryDAO.getFineByIssueId(issueId);
        if (fine != null) {
            return fine.toFormattedReceipt();
        }
        return "No overdue fine record found for Issue #" + issueId + " (returned on time).";
    }

    public boolean payFine(int fineId) throws SQLException {
        return libraryDAO.markFineAsPaid(fineId);
    }

    public List<Book> getCatalog() throws SQLException {
        return libraryDAO.getAllBooks();
    }

    public List<Book> searchCatalog(String query) throws SQLException {
        return libraryDAO.searchBooks(query);
    }

    public List<Member> getMembers() throws SQLException {
        return libraryDAO.getAllMembers();
    }

    public Member getMember(int memberId) throws SQLException {
        return libraryDAO.getMemberById(memberId);
    }

    public List<BookIssue> getAllIssues() throws SQLException {
        return libraryDAO.getAllIssues();
    }

    public List<BookIssue> getActiveIssues() throws SQLException {
        return libraryDAO.getActiveIssues();
    }

    public List<Fine> getAllFines() throws SQLException {
        return libraryDAO.getAllFines();
    }

    public List<CategoryAnalytics> getCategoryAnalytics() throws SQLException {
        return libraryDAO.getCategoryAnalytics();
    }
}
