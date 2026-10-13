package com.evoriea.dlms.dao;

import com.evoriea.dlms.config.DBConnection;
import com.evoriea.dlms.model.Book;
import com.evoriea.dlms.model.BookIssue;
import com.evoriea.dlms.model.CategoryAnalytics;
import com.evoriea.dlms.model.Fine;
import com.evoriea.dlms.model.Member;
import com.evoriea.dlms.model.MembershipType;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for the Digital Library Management System.
 * 
 * Encapsulates all SQL statements, PreparedStatement bindings to prevent SQL injection,
 * ResultSet mapping, and explicit ACID transaction management (commit/rollback).
 * 
 * Architectural Layer 3: Data Access Layer (Slide 6 & 9)
 */
public class LibraryDAO {

    private final DBConnection dbConnection;

    public LibraryDAO() {
        this.dbConnection = DBConnection.getInstance();
    }

    private Connection getConn() throws SQLException {
        return dbConnection.getConnection();
    }

    /**
     * Initializes the 3NF relational schema (BOOKS, MEMBERS, BOOK_ISSUES, FINES)
     * and B-Tree indexes if they do not exist.
     */
    public void initializeSchema() throws SQLException {
        Connection conn = getConn();
        boolean isMySQL = "mysql".equalsIgnoreCase(dbConnection.getDbType());
        String autoInc = isMySQL ? "AUTO_INCREMENT" : "AUTOINCREMENT";

        String[] schemaStatements = new String[] {
            // Table 1: BOOKS
            "CREATE TABLE IF NOT EXISTS books (" +
            "    book_id INTEGER PRIMARY KEY " + autoInc + "," +
            "    isbn VARCHAR(20) NOT NULL UNIQUE," +
            "    title VARCHAR(200) NOT NULL," +
            "    author VARCHAR(150) NOT NULL," +
            "    publisher VARCHAR(150) NOT NULL," +
            "    category VARCHAR(100) NOT NULL," +
            "    total_copies INTEGER NOT NULL CHECK (total_copies >= 0)," +
            "    available_copies INTEGER NOT NULL CHECK (available_copies >= 0 AND available_copies <= total_copies)" +
            ");",

            // Table 2: MEMBERS
            "CREATE TABLE IF NOT EXISTS members (" +
            "    member_id INTEGER PRIMARY KEY " + autoInc + "," +
            "    full_name VARCHAR(150) NOT NULL," +
            "    email VARCHAR(150) NOT NULL UNIQUE," +
            "    phone VARCHAR(20) NOT NULL," +
            "    membership_type VARCHAR(20) NOT NULL CHECK (membership_type IN ('STUDENT', 'FACULTY', 'GENERAL'))," +
            "    join_date DATE NOT NULL," +
            "    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'EXPIRED'))" +
            ");",

            // Table 3: BOOK_ISSUES
            "CREATE TABLE IF NOT EXISTS book_issues (" +
            "    issue_id INTEGER PRIMARY KEY " + autoInc + "," +
            "    book_id INTEGER NOT NULL," +
            "    member_id INTEGER NOT NULL," +
            "    issue_date DATE NOT NULL," +
            "    due_date DATE NOT NULL," +
            "    return_date DATE," +
            "    status VARCHAR(20) NOT NULL DEFAULT 'ISSUED' CHECK (status IN ('ISSUED', 'RETURNED'))," +
            "    FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE RESTRICT," +
            "    FOREIGN KEY (member_id) REFERENCES members(member_id) ON DELETE RESTRICT" +
            ");",

            // Table 4: FINES
            "CREATE TABLE IF NOT EXISTS fines (" +
            "    fine_id INTEGER PRIMARY KEY " + autoInc + "," +
            "    issue_id INTEGER NOT NULL," +
            "    member_id INTEGER NOT NULL," +
            "    amount DECIMAL(10, 2) NOT NULL CHECK (amount >= 0)," +
            "    reason VARCHAR(200) NOT NULL," +
            "    paid_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID' CHECK (paid_status IN ('UNPAID', 'PAID'))," +
            "    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "    FOREIGN KEY (issue_id) REFERENCES book_issues(issue_id) ON DELETE CASCADE," +
            "    FOREIGN KEY (member_id) REFERENCES members(member_id) ON DELETE RESTRICT" +
            ");",

            // B-Tree Indexes
            "CREATE INDEX IF NOT EXISTS idx_books_isbn ON books(isbn);",
            "CREATE INDEX IF NOT EXISTS idx_books_category ON books(category);",
            "CREATE INDEX IF NOT EXISTS idx_books_title ON books(title);",
            "CREATE INDEX IF NOT EXISTS idx_members_email ON members(email);",
            "CREATE INDEX IF NOT EXISTS idx_issues_book_id ON book_issues(book_id);",
            "CREATE INDEX IF NOT EXISTS idx_issues_member_id ON book_issues(member_id);",
            "CREATE INDEX IF NOT EXISTS idx_issues_status ON book_issues(status);",
            "CREATE INDEX IF NOT EXISTS idx_fines_issue_id ON fines(issue_id);",
            "CREATE INDEX IF NOT EXISTS idx_fines_member_id ON fines(member_id);"
        };

        try (Statement stmt = conn.createStatement()) {
            for (String sql : schemaStatements) {
                stmt.execute(sql);
            }
        }
    }

    // ========================================================================
    // BOOK OPERATIONS
    // ========================================================================

    public List<Book> getAllBooks() throws SQLException {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books ORDER BY book_id ASC;";
        Connection conn = getConn();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                books.add(mapResultSetToBook(rs));
            }
        }
        return books;
    }

    public Book getBookById(int bookId) throws SQLException {
        String sql = "SELECT * FROM books WHERE book_id = ?;";
        Connection conn = getConn();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBook(rs);
                }
            }
        }
        return null;
    }

    public Book getBookByIsbn(String isbn) throws SQLException {
        String sql = "SELECT * FROM books WHERE isbn = ?;";
        Connection conn = getConn();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, isbn.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBook(rs);
                }
            }
        }
        return null;
    }

    public List<Book> searchBooks(String query) throws SQLException {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books WHERE title LIKE ? OR author LIKE ? OR category LIKE ? OR isbn LIKE ? ORDER BY title ASC;";
        String pattern = "%" + query.trim() + "%";
        Connection conn = getConn();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            pstmt.setString(3, pattern);
            pstmt.setString(4, pattern);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    books.add(mapResultSetToBook(rs));
                }
            }
        }
        return books;
    }

    public int addBook(Book book) throws SQLException {
        String sql = "INSERT INTO books (isbn, title, author, publisher, category, total_copies, available_copies) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?);";
        Connection conn = getConn();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, book.getIsbn());
            pstmt.setString(2, book.getTitle());
            pstmt.setString(3, book.getAuthor());
            pstmt.setString(4, book.getPublisher());
            pstmt.setString(5, book.getCategory());
            pstmt.setInt(6, book.getTotalCopies());
            pstmt.setInt(7, book.getAvailableCopies());
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    book.setBookId(rs.getInt(1));
                    return book.getBookId();
                }
            }
        }
        return 0;
    }

    private Book mapResultSetToBook(ResultSet rs) throws SQLException {
        return new Book(
            rs.getInt("book_id"),
            rs.getString("isbn"),
            rs.getString("title"),
            rs.getString("author"),
            rs.getString("publisher"),
            rs.getString("category"),
            rs.getInt("total_copies"),
            rs.getInt("available_copies")
        );
    }

    // ========================================================================
    // MEMBER OPERATIONS
    // ========================================================================

    public List<Member> getAllMembers() throws SQLException {
        List<Member> members = new ArrayList<>();
        String sql = "SELECT * FROM members ORDER BY member_id ASC;";
        Connection conn = getConn();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                members.add(mapResultSetToMember(rs));
            }
        }
        return members;
    }

    public Member getMemberById(int memberId) throws SQLException {
        String sql = "SELECT * FROM members WHERE member_id = ?;";
        Connection conn = getConn();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, memberId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMember(rs);
                }
            }
        }
        return null;
    }

    public Member getMemberByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM members WHERE email = ?;";
        Connection conn = getConn();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMember(rs);
                }
            }
        }
        return null;
    }

    public int addMember(Member member) throws SQLException {
        String sql = "INSERT INTO members (full_name, email, phone, membership_type, join_date, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?);";
        Connection conn = getConn();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, member.getFullName());
            pstmt.setString(2, member.getEmail());
            pstmt.setString(3, member.getPhone());
            pstmt.setString(4, member.getMembershipType().name());
            pstmt.setDate(5, Date.valueOf(member.getJoinDate()));
            pstmt.setString(6, member.getStatus());
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    member.setMemberId(rs.getInt(1));
                    return member.getMemberId();
                }
            }
        }
        return 0;
    }

    public int getActiveLoanCount(int memberId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM book_issues WHERE member_id = ? AND status = 'ISSUED';";
        Connection conn = getConn();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, memberId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private Member mapResultSetToMember(ResultSet rs) throws SQLException {
        Date joinDateVal = rs.getDate("join_date");
        LocalDate joinDate = joinDateVal != null ? joinDateVal.toLocalDate() : LocalDate.now();
        return new Member(
            rs.getInt("member_id"),
            rs.getString("full_name"),
            rs.getString("email"),
            rs.getString("phone"),
            MembershipType.fromString(rs.getString("membership_type")),
            joinDate,
            rs.getString("status")
        );
    }

    // ========================================================================
    // ATOMIC TRANSACTION: ISSUE BOOK (Slide 8 & 9)
    // ========================================================================

    /**
     * Executes the atomic book issue transaction:
     * 1. Validates member status and borrowing limit (STUDENT: 3, FACULTY: 5, GENERAL: 2)
     * 2. Validates book availability (available_copies > 0)
     * 3. Sets autoCommit to false
     * 4. Decrements available_copies in BOOKS
     * 5. Inserts new record in BOOK_ISSUES with computed due date
     * 6. Commits transaction; rolls back on failure
     */
    public BookIssue issueBook(int memberId, int bookId, LocalDate issueDate) throws SQLException {
        Connection conn = getConn();
        boolean originalAutoCommit = conn.getAutoCommit();

        try {
            // Validation prior to transaction
            Member member = getMemberById(memberId);
            if (member == null) {
                throw new IllegalArgumentException("Member ID " + memberId + " not found.");
            }
            if (!"ACTIVE".equalsIgnoreCase(member.getStatus())) {
                throw new IllegalStateException("Member account is " + member.getStatus() + ". Cannot issue books.");
            }

            int activeLoans = getActiveLoanCount(memberId);
            int maxLimit = member.getMembershipType().getBorrowingLimit();
            if (activeLoans >= maxLimit) {
                throw new IllegalStateException(String.format(
                    "Borrowing limit reached for %s (%s). Current active loans: %d / Limit: %d",
                    member.getFullName(), member.getMembershipType(), activeLoans, maxLimit));
            }

            Book book = getBookById(bookId);
            if (book == null) {
                throw new IllegalArgumentException("Book ID " + bookId + " not found.");
            }
            if (book.getAvailableCopies() <= 0) {
                throw new IllegalStateException("Book '" + book.getTitle() + "' is currently out of stock (0 available).");
            }

            // Compute due date based on membership type (Slide 8)
            int loanDays = member.getMembershipType().getLoanPeriodDays();
            LocalDate dueDate = issueDate.plusDays(loanDays);

            // BEGIN ATOMIC TRANSACTION (Slide 9)
            conn.setAutoCommit(false);

            // Step 1: Decrement stock
            String updateStockSql = "UPDATE books SET available_copies = available_copies - 1 WHERE book_id = ? AND available_copies > 0;";
            try (PreparedStatement pstmtStock = conn.prepareStatement(updateStockSql)) {
                pstmtStock.setInt(1, bookId);
                int rowsUpdated = pstmtStock.executeUpdate();
                if (rowsUpdated == 0) {
                    throw new SQLException("Failed to decrement book stock. Concurrency conflict or zero availability.");
                }
            }

            // Step 2: Insert into book_issues
            String insertIssueSql = "INSERT INTO book_issues (book_id, member_id, issue_date, due_date, status) VALUES (?, ?, ?, ?, 'ISSUED');";
            int generatedIssueId = 0;
            try (PreparedStatement pstmtIssue = conn.prepareStatement(insertIssueSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmtIssue.setInt(1, bookId);
                pstmtIssue.setInt(2, memberId);
                pstmtIssue.setDate(3, Date.valueOf(issueDate));
                pstmtIssue.setDate(4, Date.valueOf(dueDate));
                pstmtIssue.executeUpdate();
                try (ResultSet keys = pstmtIssue.getGeneratedKeys()) {
                    if (keys.next()) {
                        generatedIssueId = keys.getInt(1);
                    }
                }
            }

            // COMMIT ATOMIC TRANSACTION
            conn.commit();

            BookIssue issue = new BookIssue(generatedIssueId, bookId, memberId, issueDate, dueDate, null, "ISSUED");
            issue.setBookTitle(book.getTitle());
            issue.setIsbn(book.getIsbn());
            issue.setMemberName(member.getFullName());
            issue.setMembershipType(member.getMembershipType());
            return issue;

        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                // suppress secondary rollback exception
            }
            throw new SQLException("Transaction rolled back: " + e.getMessage(), e);
        } finally {
            try {
                conn.setAutoCommit(originalAutoCommit);
            } catch (SQLException ignored) {}
        }
    }

    // ========================================================================
    // ATOMIC TRANSACTION: RETURN BOOK & FINE ENGINE (Slide 8, 9 & 10)
    // ========================================================================

    /**
     * Executes the atomic book return transaction:
     * 1. Validates issue record exists and is in 'ISSUED' status
     * 2. Computes overdue days: Return Date - Due Date (if positive)
     * 3. Calculates overdue fine: Overdue Days * Rs. 5.00/day
     * 4. Increments book stock (available_copies)
     * 5. Updates BOOK_ISSUES with return_date and status = 'RETURNED'
     * 6. Inserts record into FINES if fine > 0 (marked 'UNPAID')
     * 7. Commits transaction; rolls back on failure
     */
    public Fine returnBook(int issueId, LocalDate returnDate) throws SQLException {
        Connection conn = getConn();
        boolean originalAutoCommit = conn.getAutoCommit();

        try {
            BookIssue issue = getIssueById(issueId);
            if (issue == null) {
                throw new IllegalArgumentException("Issue record #" + issueId + " not found.");
            }
            if ("RETURNED".equalsIgnoreCase(issue.getStatus())) {
                throw new IllegalStateException("Issue record #" + issueId + " has already been returned.");
            }

            LocalDate dueDate = issue.getDueDate();
            long overdueDays = 0;
            if (returnDate.isAfter(dueDate)) {
                overdueDays = ChronoUnit.DAYS.between(dueDate, returnDate);
            }
            double fineAmount = overdueDays * Fine.FINE_RATE_PER_DAY;

            // BEGIN ATOMIC TRANSACTION (Slide 9)
            conn.setAutoCommit(false);

            // Step 1: Update book_issues
            String updateIssueSql = "UPDATE book_issues SET return_date = ?, status = 'RETURNED' WHERE issue_id = ?;";
            try (PreparedStatement pstmtIssue = conn.prepareStatement(updateIssueSql)) {
                pstmtIssue.setDate(1, Date.valueOf(returnDate));
                pstmtIssue.setInt(2, issueId);
                pstmtIssue.executeUpdate();
            }

            // Step 2: Restore book stock
            String updateStockSql = "UPDATE books SET available_copies = available_copies + 1 WHERE book_id = ?;";
            try (PreparedStatement pstmtStock = conn.prepareStatement(updateStockSql)) {
                pstmtStock.setInt(1, issue.getBookId());
                pstmtStock.executeUpdate();
            }

            Fine createdFine = null;
            // Step 3: Insert fine if overdueDays > 0
            if (fineAmount > 0) {
                String insertFineSql = "INSERT INTO fines (issue_id, member_id, amount, reason, paid_status, created_at) VALUES (?, ?, ?, ?, 'UNPAID', ?);";
                int generatedFineId = 0;
                Timestamp now = Timestamp.valueOf(LocalDate.now().atStartOfDay());
                String reason = String.format("Overdue return: %d days past due date %s (Rs. 5.00/day)", overdueDays, dueDate);
                try (PreparedStatement pstmtFine = conn.prepareStatement(insertFineSql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmtFine.setInt(1, issueId);
                    pstmtFine.setInt(2, issue.getMemberId());
                    pstmtFine.setDouble(3, fineAmount);
                    pstmtFine.setString(4, reason);
                    pstmtFine.setTimestamp(5, now);
                    pstmtFine.executeUpdate();
                    try (ResultSet keys = pstmtFine.getGeneratedKeys()) {
                        if (keys.next()) {
                            generatedFineId = keys.getInt(1);
                        }
                    }
                }
                createdFine = new Fine(generatedFineId, issueId, issue.getMemberId(), fineAmount, reason, "UNPAID", now.toLocalDateTime());
                createdFine.setMemberName(issue.getMemberName());
                createdFine.setMembershipType(issue.getMembershipType());
                createdFine.setBookTitle(issue.getBookTitle());
                createdFine.setIsbn(issue.getIsbn());
                createdFine.setIssueDate(issue.getIssueDate());
                createdFine.setDueDate(issue.getDueDate());
                createdFine.setReturnDate(returnDate);
                createdFine.setOverdueDays(overdueDays);
            }

            // COMMIT ATOMIC TRANSACTION
            conn.commit();

            return createdFine;

        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                // suppress secondary rollback exception
            }
            throw new SQLException("Transaction rolled back: " + e.getMessage(), e);
        } finally {
            try {
                conn.setAutoCommit(originalAutoCommit);
            } catch (SQLException ignored) {}
        }
    }

    // ========================================================================
    // ISSUES QUERY OPERATIONS
    // ========================================================================

    public BookIssue getIssueById(int issueId) throws SQLException {
        String sql = "SELECT bi.*, b.title AS book_title, b.isbn, m.full_name AS member_name, m.membership_type " +
                     "FROM book_issues bi " +
                     "JOIN books b ON bi.book_id = b.book_id " +
                     "JOIN members m ON bi.member_id = m.member_id " +
                     "WHERE bi.issue_id = ?;";
        Connection conn = getConn();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, issueId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBookIssue(rs);
                }
            }
        }
        return null;
    }

    public List<BookIssue> getAllIssues() throws SQLException {
        List<BookIssue> issues = new ArrayList<>();
        String sql = "SELECT bi.*, b.title AS book_title, b.isbn, m.full_name AS member_name, m.membership_type " +
                     "FROM book_issues bi " +
                     "JOIN books b ON bi.book_id = b.book_id " +
                     "JOIN members m ON bi.member_id = m.member_id " +
                     "ORDER BY bi.issue_id DESC;";
        Connection conn = getConn();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                issues.add(mapResultSetToBookIssue(rs));
            }
        }
        return issues;
    }

    public List<BookIssue> getActiveIssues() throws SQLException {
        List<BookIssue> issues = new ArrayList<>();
        String sql = "SELECT bi.*, b.title AS book_title, b.isbn, m.full_name AS member_name, m.membership_type " +
                     "FROM book_issues bi " +
                     "JOIN books b ON bi.book_id = b.book_id " +
                     "JOIN members m ON bi.member_id = m.member_id " +
                     "WHERE bi.status = 'ISSUED' " +
                     "ORDER BY bi.due_date ASC;";
        Connection conn = getConn();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                issues.add(mapResultSetToBookIssue(rs));
            }
        }
        return issues;
    }

    public List<BookIssue> getIssuesByMember(int memberId) throws SQLException {
        List<BookIssue> issues = new ArrayList<>();
        String sql = "SELECT bi.*, b.title AS book_title, b.isbn, m.full_name AS member_name, m.membership_type " +
                     "FROM book_issues bi " +
                     "JOIN books b ON bi.book_id = b.book_id " +
                     "JOIN members m ON bi.member_id = m.member_id " +
                     "WHERE bi.member_id = ? " +
                     "ORDER BY bi.issue_id DESC;";
        Connection conn = getConn();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, memberId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    issues.add(mapResultSetToBookIssue(rs));
                }
            }
        }
        return issues;
    }

    private BookIssue mapResultSetToBookIssue(ResultSet rs) throws SQLException {
        Date issueDateVal = rs.getDate("issue_date");
        Date dueDateVal = rs.getDate("due_date");
        Date returnDateVal = rs.getDate("return_date");

        BookIssue issue = new BookIssue(
            rs.getInt("issue_id"),
            rs.getInt("book_id"),
            rs.getInt("member_id"),
            issueDateVal != null ? issueDateVal.toLocalDate() : null,
            dueDateVal != null ? dueDateVal.toLocalDate() : null,
            returnDateVal != null ? returnDateVal.toLocalDate() : null,
            rs.getString("status")
        );
        issue.setBookTitle(rs.getString("book_title"));
        issue.setIsbn(rs.getString("isbn"));
        issue.setMemberName(rs.getString("member_name"));
        issue.setMembershipType(MembershipType.fromString(rs.getString("membership_type")));
        return issue;
    }

    // ========================================================================
    // FINE OPERATIONS & AUDIT TRAIL
    // ========================================================================

    public List<Fine> getAllFines() throws SQLException {
        List<Fine> fines = new ArrayList<>();
        String sql = "SELECT f.*, bi.issue_date, bi.due_date, bi.return_date, b.title AS book_title, b.isbn, m.full_name AS member_name, m.membership_type " +
                     "FROM fines f " +
                     "JOIN book_issues bi ON f.issue_id = bi.issue_id " +
                     "JOIN books b ON bi.book_id = b.book_id " +
                     "JOIN members m ON f.member_id = m.member_id " +
                     "ORDER BY f.fine_id DESC;";
        Connection conn = getConn();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                fines.add(mapResultSetToFine(rs));
            }
        }
        return fines;
    }

    public Fine getFineByIssueId(int issueId) throws SQLException {
        String sql = "SELECT f.*, bi.issue_date, bi.due_date, bi.return_date, b.title AS book_title, b.isbn, m.full_name AS member_name, m.membership_type " +
                     "FROM fines f " +
                     "JOIN book_issues bi ON f.issue_id = bi.issue_id " +
                     "JOIN books b ON bi.book_id = b.book_id " +
                     "JOIN members m ON f.member_id = m.member_id " +
                     "WHERE f.issue_id = ?;";
        Connection conn = getConn();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, issueId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToFine(rs);
                }
            }
        }
        return null;
    }

    public boolean markFineAsPaid(int fineId) throws SQLException {
        String sql = "UPDATE fines SET paid_status = 'PAID' WHERE fine_id = ? AND paid_status = 'UNPAID';";
        Connection conn = getConn();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, fineId);
            return pstmt.executeUpdate() > 0;
        }
    }

    private Fine mapResultSetToFine(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        Date issueDateVal = rs.getDate("issue_date");
        Date dueDateVal = rs.getDate("due_date");
        Date returnDateVal = rs.getDate("return_date");

        LocalDate dueDate = dueDateVal != null ? dueDateVal.toLocalDate() : null;
        LocalDate returnDate = returnDateVal != null ? returnDateVal.toLocalDate() : null;
        long overdueDays = 0;
        if (dueDate != null && returnDate != null && returnDate.isAfter(dueDate)) {
            overdueDays = ChronoUnit.DAYS.between(dueDate, returnDate);
        }

        Fine fine = new Fine(
            rs.getInt("fine_id"),
            rs.getInt("issue_id"),
            rs.getInt("member_id"),
            rs.getDouble("amount"),
            rs.getString("reason"),
            rs.getString("paid_status"),
            ts != null ? ts.toLocalDateTime() : null
        );
        fine.setBookTitle(rs.getString("book_title"));
        fine.setIsbn(rs.getString("isbn"));
        fine.setMemberName(rs.getString("member_name"));
        fine.setMembershipType(MembershipType.fromString(rs.getString("membership_type")));
        fine.setIssueDate(issueDateVal != null ? issueDateVal.toLocalDate() : null);
        fine.setDueDate(dueDate);
        fine.setReturnDate(returnDate);
        fine.setOverdueDays(overdueDays);
        return fine;
    }

    // ========================================================================
    // SQL ANALYTICS: CATEGORY-WISE CIRCULATION & FINES (Slide 11)
    // ========================================================================

    /**
     * Executes the category-wise circulation and fine aggregation query matching Slide 11:
     * - Computer Science: 18 issued, Rs. 180.00 fine, 31.0% share
     * - Engineering Mathematics: 14 issued, Rs. 110.00 fine, 24.1% share
     * - Fiction: 11 issued, Rs. 45.00 fine, 19.0% share
     * - General Reference: 9 issued, Rs. 35.00 fine, 15.5% share
     * - Biography: 6 issued, Rs. 20.00 fine, 10.3% share
     */
    public List<CategoryAnalytics> getCategoryAnalytics() throws SQLException {
        List<CategoryAnalytics> analyticsList = new ArrayList<>();
        Connection conn = getConn();

        // Total issue count subquery for percentage calculation
        String totalIssuesSql = "SELECT COUNT(*) FROM book_issues;";
        int totalIssues = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rsTotal = stmt.executeQuery(totalIssuesSql)) {
            if (rsTotal.next()) {
                totalIssues = rsTotal.getInt(1);
            }
        }
        if (totalIssues == 0) totalIssues = 1; // avoid divide by zero

        String sql = "SELECT " +
                     "    b.category AS book_category, " +
                     "    COUNT(bi.issue_id) AS books_issued, " +
                     "    COALESCE(SUM(f.amount), 0.0) AS fine_collected " +
                     "FROM books b " +
                     "JOIN book_issues bi ON b.book_id = bi.book_id " +
                     "LEFT JOIN fines f ON bi.issue_id = f.issue_id " +
                     "GROUP BY b.category " +
                     "ORDER BY books_issued DESC, fine_collected DESC;";

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String cat = rs.getString("book_category");
                int count = rs.getInt("books_issued");
                double fine = rs.getDouble("fine_collected");
                double share = Math.round((count * 100.0 / totalIssues) * 10.0) / 10.0;
                analyticsList.add(new CategoryAnalytics(cat, count, fine, share));
            }
        }
        return analyticsList;
    }

    public int getBookCount() throws SQLException {
        return getCount("SELECT COUNT(*) FROM books;");
    }

    public int getMemberCount() throws SQLException {
        return getCount("SELECT COUNT(*) FROM members;");
    }

    public int getIssueCount() throws SQLException {
        return getCount("SELECT COUNT(*) FROM book_issues;");
    }

    public int getFineCount() throws SQLException {
        return getCount("SELECT COUNT(*) FROM fines;");
    }

    private int getCount(String sql) throws SQLException {
        Connection conn = getConn();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
}
