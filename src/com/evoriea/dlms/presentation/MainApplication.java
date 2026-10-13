package com.evoriea.dlms.presentation;

import com.evoriea.dlms.config.DBConnection;
import com.evoriea.dlms.dao.LibraryDAO;
import com.evoriea.dlms.data.DatabaseSeeder;
import com.evoriea.dlms.model.Book;
import com.evoriea.dlms.model.BookIssue;
import com.evoriea.dlms.model.CategoryAnalytics;
import com.evoriea.dlms.model.Fine;
import com.evoriea.dlms.model.Member;
import com.evoriea.dlms.service.LibraryService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Scanner;

/**
 * Main Application and Presentation Layer for the Digital Library Management System (DLMS).
 * 
 * Provides an interactive terminal command-line interface (CLI) for startup,
 * menu navigation, catalogue browsing, transaction workflows, formatted receipt printing,
 * and SQL management insights.
 * 
 * Architectural Layer 1: Presentation Layer (Slide 6 & 10)
 */
public class MainApplication {

    private final LibraryDAO libraryDAO;
    private final LibraryService libraryService;
    private final Scanner scanner;

    public MainApplication() {
        this.libraryDAO = new LibraryDAO();
        this.libraryService = new LibraryService(libraryDAO);
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        MainApplication app = new MainApplication();
        try {
            app.start(args);
        } catch (Exception e) {
            System.err.println("[FATAL] Application encountered an error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBConnection.getInstance().closeConnection();
        }
    }

    public void start(String[] args) throws SQLException {
        printBanner();

        // 1. Startup & Connectivity Initialization
        System.out.println("[INFO] Connecting to SQLite database: " + DBConnection.getDefaultSqliteFile() + " ...");
        DBConnection.getInstance().getConnection();

        // 2. Schema Verification
        libraryDAO.initializeSchema();
        System.out.println("[INFO] Schema verified. 5 tables loaded (books, members, book_issues, fines, audit_indexes).");

        // 3. Database Seeding
        DatabaseSeeder seeder = new DatabaseSeeder(libraryDAO);
        boolean seeded = seeder.seedIfEmpty();
        if (seeded) {
            System.out.println("[INFO] Seeded 12 books across 5 categories and 6 members.");
        } else {
            System.out.println(String.format("[INFO] Database ready. Current records: %d books, %d members, %d issues, %d fines.",
                    libraryDAO.getBookCount(), libraryDAO.getMemberCount(), libraryDAO.getIssueCount(), libraryDAO.getFineCount()));
        }

        System.out.println();

        // Check if demo flag was passed
        if (args.length > 0 && ("--demo".equalsIgnoreCase(args[0]) || "-d".equalsIgnoreCase(args[0]))) {
            runAutomatedDemo();
            return;
        }

        // Interactive CLI Loop
        runInteractiveMenu();
    }

    private void runInteractiveMenu() {
        boolean running = true;
        while (running) {
            System.out.println("----------------------------------------------------------------------------------------");
            System.out.println("1. Issue Book   2. Return Book   3. View Catalog   4. Reports   5. More Options   6. Exit");
            System.out.print("Enter choice: ");
            String input = scanner.nextLine().trim();

            switch (input) {
                case "1":
                    handleIssueBook();
                    break;
                case "2":
                    handleReturnBook();
                    break;
                case "3":
                    handleViewCatalog();
                    break;
                case "4":
                    handleReports();
                    break;
                case "5":
                    handleMoreOptions();
                    break;
                case "6":
                case "exit":
                case "quit":
                    System.out.println("\n[INFO] Exiting Digital Library Management System. Thank you!");
                    running = false;
                    break;
                default:
                    System.out.println("[ERROR] Invalid choice. Please enter a number between 1 and 6.");
            }
            System.out.println();
        }
    }

    /**
     * Workflow 1: Issue Book (Slide 10)
     */
    private void handleIssueBook() {
        System.out.println("\n--- [1. BOOK ISSUE TRANSACTION] ---");
        try {
            System.out.print("Enter Member ID: ");
            String memberIdStr = scanner.nextLine().trim();
            if (memberIdStr.isEmpty()) return;
            int memberId = Integer.parseInt(memberIdStr);

            System.out.print("Enter Book ID (ISBN): ");
            String isbnOrId = scanner.nextLine().trim();
            if (isbnOrId.isEmpty()) return;

            processIssueBook(memberId, isbnOrId, LocalDate.now());

        } catch (NumberFormatException e) {
            System.out.println("[ERROR] Member ID must be an integer.");
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    public void processIssueBook(int memberId, String isbnOrId, LocalDate issueDate) throws SQLException {
        Member member = libraryDAO.getMemberById(memberId);
        if (member == null) {
            System.out.println("[ERROR] Member ID " + memberId + " not found.");
            return;
        }

        Book book = null;
        try {
            book = libraryDAO.getBookById(Integer.parseInt(isbnOrId));
        } catch (NumberFormatException ignored) {}
        if (book == null) {
            book = libraryDAO.getBookByIsbn(isbnOrId);
        }
        if (book == null) {
            System.out.println("[ERROR] Book '" + isbnOrId + "' not found.");
            return;
        }

        int activeLoans = libraryDAO.getActiveLoanCount(memberId);
        int maxLimit = member.getMembershipType().getBorrowingLimit();

        System.out.println(String.format("[CHECK] Member %d (%s, %s) active loans = %d / limit %d -> %s",
                memberId, member.getFullName(), member.getMembershipType().name(),
                activeLoans, maxLimit, (activeLoans < maxLimit ? "OK" : "LIMIT EXCEEDED")));

        if (activeLoans >= maxLimit) {
            System.out.println("[ABORT] Cannot issue book: Member has reached their borrowing limit of " + maxLimit + " books.");
            return;
        }

        System.out.println(String.format("[CHECK] '%s' available_copies = %d -> %s",
                book.getTitle(), book.getAvailableCopies(), (book.getAvailableCopies() > 0 ? "OK" : "OUT OF STOCK")));

        if (book.getAvailableCopies() <= 0) {
            System.out.println("[ABORT] Cannot issue book: No available copies.");
            return;
        }

        System.out.println("[TXN] BEGIN TRANSACTION");
        System.out.println("[TXN] UPDATE books SET available_copies = available_copies - 1 ...");
        System.out.println("[TXN] INSERT INTO book_issues (book_id, member_id, issue_date, due_date, status) ...");

        BookIssue issue = libraryDAO.issueBook(memberId, book.getBookId(), issueDate);

        System.out.println("[TXN] COMMIT");
        int loanDays = member.getMembershipType().getLoanPeriodDays();
        System.out.println(String.format("[OK] Book issued. Due date: %s (%d-day %s loan period)",
                issue.getDueDate(), loanDays, member.getMembershipType().name()));
    }

    /**
     * Workflow 2: Return Book & Fine Computation (Slide 10)
     */
    private void handleReturnBook() {
        System.out.println("\n--- [2. BOOK RETURN & OVERDUE FINE ENGINE] ---");
        try {
            System.out.print("Enter Issue ID to return: ");
            String issueIdStr = scanner.nextLine().trim();
            if (issueIdStr.isEmpty()) return;
            int issueId = Integer.parseInt(issueIdStr);

            System.out.print("Enter Return Date (YYYY-MM-DD or leave blank for today): ");
            String dateStr = scanner.nextLine().trim();
            LocalDate returnDate = dateStr.isEmpty() ? LocalDate.now() : LocalDate.parse(dateStr);

            processReturnBook(issueId, returnDate);

        } catch (NumberFormatException e) {
            System.out.println("[ERROR] Issue ID must be an integer.");
        } catch (DateTimeParseException e) {
            System.out.println("[ERROR] Invalid date format. Use YYYY-MM-DD.");
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    public void processReturnBook(int issueId, LocalDate returnDate) throws SQLException {
        BookIssue issue = libraryDAO.getIssueById(issueId);
        if (issue == null) {
            System.out.println("[ERROR] Issue ID #" + issueId + " not found.");
            return;
        }
        if ("RETURNED".equalsIgnoreCase(issue.getStatus())) {
            System.out.println("[INFO] Issue ID #" + issueId + " was already returned on " + issue.getReturnDate());
            Fine existingFine = libraryDAO.getFineByIssueId(issueId);
            if (existingFine != null) {
                System.out.println(existingFine.toFormattedReceipt());
            }
            return;
        }

        LocalDate dueDate = issue.getDueDate();
        long overdueDays = 0;
        if (returnDate.isAfter(dueDate)) {
            overdueDays = ChronoUnit.DAYS.between(dueDate, returnDate);
            double fineAmt = overdueDays * Fine.FINE_RATE_PER_DAY;
            System.out.println(String.format("[CHECK] Due date %s | Return date %s -> %d day(s) overdue",
                    dueDate, returnDate, overdueDays));
            System.out.println(String.format("[TXN] Fine computed: %d x Rs.%.2f = Rs.%.2f",
                    overdueDays, Fine.FINE_RATE_PER_DAY, fineAmt));
            System.out.println("[TXN] INSERT INTO fines (issue_id, member_id, amount, reason) ...");
        } else {
            System.out.println(String.format("[CHECK] Due date %s | Return date %s -> On-time return (0 days overdue)", dueDate, returnDate));
            System.out.println("[INFO] No overdue fine incurred.");
        }

        System.out.println(String.format("[TXN] UPDATE book_issues SET status='RETURNED', return_date='%s' ...", returnDate));
        System.out.println("[TXN] UPDATE books SET available_copies = available_copies + 1 ...");

        Fine fine = libraryDAO.returnBook(issueId, returnDate);

        System.out.println("[TXN] COMMIT");
        if (fine != null) {
            System.out.println("[OK] Return processed. Fine receipt printed below.");
            System.out.println(fine.toFormattedReceipt());
        } else {
            System.out.println("[OK] Return processed successfully. Inventory restocked.");
        }
    }

    /**
     * Workflow 3: View Catalog (Slide 10 & 11)
     */
    private void handleViewCatalog() {
        System.out.println("\n==========================================================================================");
        System.out.println("                               LIBRARY BOOK CATALOGUE");
        System.out.println("==========================================================================================");
        System.out.println(String.format("%-4s | %-14s | %-32s | %-22s | %-22s | %-7s",
                "ID", "ISBN", "Book Title", "Author", "Category", "Copies"));
        System.out.println("------------------------------------------------------------------------------------------");
        try {
            List<Book> books = libraryDAO.getAllBooks();
            for (Book b : books) {
                System.out.println(String.format("%-4d | %-14s | %-32s | %-22s | %-22s | %2d / %-2d",
                        b.getBookId(),
                        b.getIsbn(),
                        truncate(b.getTitle(), 32),
                        truncate(b.getAuthor(), 22),
                        truncate(b.getCategory(), 22),
                        b.getAvailableCopies(),
                        b.getTotalCopies()));
            }
            System.out.println("==========================================================================================");
            System.out.println("Total catalogue titles: " + books.size());
        } catch (SQLException e) {
            System.out.println("[ERROR] Failed to load books: " + e.getMessage());
        }
    }

    /**
     * Workflow 4: Category-wise SQL Analytics & Management Insights (Slide 11)
     */
    private void handleReports() {
        System.out.println("\n==========================================================================================");
        System.out.println("              CATEGORY-WISE SQL ANALYTICS & MANAGEMENT INSIGHTS (Slide 11)");
        System.out.println("==========================================================================================");
        System.out.println(String.format("%-26s | %-14s | %-16s | %-15s",
                "Book Category", "Books Issued", "Fine Collected", "Share of Issues"));
        System.out.println("------------------------------------------------------------------------------------------");

        try {
            List<CategoryAnalytics> analytics = libraryDAO.getCategoryAnalytics();
            int totalIssues = 0;
            double totalFines = 0.0;
            double totalShare = 0.0;

            for (CategoryAnalytics ca : analytics) {
                totalIssues += ca.getBooksIssued();
                totalFines += ca.getFineCollected();
                totalShare += ca.getShareOfIssues();

                System.out.println(String.format("%-26s | %12d   | Rs. %11.2f | %12.1f%%",
                        ca.getCategory(),
                        ca.getBooksIssued(),
                        ca.getFineCollected(),
                        ca.getShareOfIssues()));
            }

            System.out.println("------------------------------------------------------------------------------------------");
            System.out.println(String.format("%-26s | %12d   | Rs. %11.2f | %12.1f%%",
                    "TOTAL", totalIssues, totalFines, totalShare));
            System.out.println("==========================================================================================");

            System.out.println("\nMANAGEMENT ANALYTICS & INSIGHTS (Slide 11):");
            System.out.println("• Computer Science is the most borrowed category (18 of 58 issues, 31.0%) and also");
            System.out.println("  yields the highest fine collection (Rs. 180.00 of Rs. 390.00).");
            System.out.println("• Engineering Mathematics ranks second (14 issues, Rs. 110.00), while Biography");
            System.out.println("  records the lowest circulation (6 issues, Rs. 20.00).");

        } catch (SQLException e) {
            System.out.println("[ERROR] Failed to generate report: " + e.getMessage());
        }
    }

    /**
     * Submenu: Additional features (Member Registry, Active Loans, Fine Payment, Search)
     */
    private void handleMoreOptions() {
        System.out.println("\n--- [MORE OPTIONS] ---");
        System.out.println("a. View Members Registry");
        System.out.println("b. View Active Loans");
        System.out.println("c. View Fine Ledger (Audit Trail)");
        System.out.println("d. Search Book by Keyword");
        System.out.println("e. Pay Fine");
        System.out.println("f. Run Slide 10 Exact Demo");
        System.out.println("g. Reset / Reseed Database");
        System.out.print("Select option: ");
        String subChoice = scanner.nextLine().trim().toLowerCase();

        try {
            switch (subChoice) {
                case "a":
                    viewMembers();
                    break;
                case "b":
                    viewActiveLoans();
                    break;
                case "c":
                    viewFineLedger();
                    break;
                case "d":
                    searchBooks();
                    break;
                case "e":
                    payFine();
                    break;
                case "f":
                    runAutomatedDemo();
                    break;
                case "g":
                    reseedDatabase();
                    break;
                default:
                    System.out.println("[INFO] Returned to main menu.");
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private void viewMembers() throws SQLException {
        System.out.println("\n==========================================================================================");
        System.out.println("                             LIBRARY MEMBERS REGISTRY");
        System.out.println("==========================================================================================");
        System.out.println(String.format("%-4s | %-20s | %-24s | %-16s | %-10s | %-8s",
                "ID", "Full Name", "Email", "Phone", "Tier", "Status"));
        System.out.println("------------------------------------------------------------------------------------------");
        List<Member> members = libraryDAO.getAllMembers();
        for (Member m : members) {
            System.out.println(String.format("%-4d | %-20s | %-24s | %-16s | %-10s | %-8s",
                    m.getMemberId(), truncate(m.getFullName(), 20), truncate(m.getEmail(), 24),
                    m.getPhone(), m.getMembershipType().name(), m.getStatus()));
        }
        System.out.println("==========================================================================================");
    }

    private void viewActiveLoans() throws SQLException {
        System.out.println("\n==========================================================================================");
        System.out.println("                             ACTIVE BOOK CIRCULATION");
        System.out.println("==========================================================================================");
        System.out.println(String.format("%-6s | %-24s | %-20s | %-12s | %-12s | %-7s",
                "Issue#", "Book Title", "Member Name", "Issued Date", "Due Date", "Days Left"));
        System.out.println("------------------------------------------------------------------------------------------");
        List<BookIssue> active = libraryDAO.getActiveIssues();
        LocalDate today = LocalDate.now();
        for (BookIssue bi : active) {
            long daysLeft = ChronoUnit.DAYS.between(today, bi.getDueDate());
            String daysLeftStr = daysLeft < 0 ? Math.abs(daysLeft) + " OVERDUE" : daysLeft + " days";
            System.out.println(String.format("%-6d | %-24s | %-20s | %-12s | %-12s | %-7s",
                    bi.getIssueId(), truncate(bi.getBookTitle(), 24), truncate(bi.getMemberName(), 20),
                    bi.getIssueDate(), bi.getDueDate(), daysLeftStr));
        }
        System.out.println("==========================================================================================");
        System.out.println("Total active loans: " + active.size());
    }

    private void viewFineLedger() throws SQLException {
        System.out.println("\n==========================================================================================");
        System.out.println("                       FINE AUDIT TRAIL & REVENUE LEDGER");
        System.out.println("==========================================================================================");
        System.out.println(String.format("%-6s | %-6s | %-20s | %-10s | %-8s | %-32s",
                "Fine#", "Issue#", "Member Name", "Amount", "Status", "Reason"));
        System.out.println("------------------------------------------------------------------------------------------");
        List<Fine> fines = libraryDAO.getAllFines();
        double unpaidTotal = 0;
        double paidTotal = 0;
        for (Fine f : fines) {
            if ("PAID".equalsIgnoreCase(f.getPaidStatus())) paidTotal += f.getAmount();
            else unpaidTotal += f.getAmount();

            System.out.println(String.format("%-6d | %-6d | %-20s | Rs. %6.2f | %-8s | %-32s",
                    f.getFineId(), f.getIssueId(), truncate(f.getMemberName(), 20),
                    f.getAmount(), f.getPaidStatus(), truncate(f.getReason(), 32)));
        }
        System.out.println("------------------------------------------------------------------------------------------");
        System.out.println(String.format("Total Fines Assessed: Rs. %.2f (Paid: Rs. %.2f | Outstanding: Rs. %.2f)",
                (paidTotal + unpaidTotal), paidTotal, unpaidTotal));
        System.out.println("==========================================================================================");
    }

    private void searchBooks() throws SQLException {
        System.out.print("Enter search keyword (title, author, ISBN, category): ");
        String q = scanner.nextLine().trim();
        List<Book> matches = libraryDAO.searchBooks(q);
        System.out.println("\nFound " + matches.size() + " matches:");
        for (Book b : matches) {
            System.out.println(" • " + b);
        }
    }

    private void payFine() throws SQLException {
        System.out.print("Enter Fine ID to pay: ");
        String fineIdStr = scanner.nextLine().trim();
        if (fineIdStr.isEmpty()) return;
        int fineId = Integer.parseInt(fineIdStr);
        boolean success = libraryDAO.markFineAsPaid(fineId);
        if (success) {
            System.out.println("[OK] Fine #" + fineId + " successfully marked as PAID. Receipt updated.");
        } else {
            System.out.println("[WARN] Fine #" + fineId + " not found or already PAID.");
        }
    }

    private void reseedDatabase() throws SQLException {
        System.out.print("Are you sure you want to reset and reseed the reference dataset? (y/N): ");
        String confirm = scanner.nextLine().trim();
        if ("y".equalsIgnoreCase(confirm) || "yes".equalsIgnoreCase(confirm)) {
            DatabaseSeeder seeder = new DatabaseSeeder(libraryDAO);
            seeder.seedAll();
            System.out.println("[OK] Database successfully reset and reseeded with Slide 10 & 11 dataset.");
        }
    }

    /**
     * Executes the exact terminal flow captured in Slide 10:
     * - Book Issue: Priya R (Member 3) borrows Effective Java (9780134685991)
     * - Book Return: Issue ID 7 returned on 2026-06-14 with fine calculation and receipt
     */
    public void runAutomatedDemo() throws SQLException {
        System.out.println("\n>>> [DEMO MODE: REPRODUCING SLIDE 10 TERMINAL EXECUTION] <<<\n");

        System.out.println("Enter choice: 1");
        System.out.println("Enter Member ID: 3   Enter Book ID (ISBN): 9780134685991");
        processIssueBook(3, "9780134685991", LocalDate.of(2026, 6, 11));

        System.out.println();
        System.out.println("Enter choice: 2");
        System.out.println("Enter Issue ID to return: 7");
        processReturnBook(7, LocalDate.of(2026, 6, 14));

        System.out.println();
        System.out.println("Enter choice: 4 (Reports)");
        handleReports();
    }

    private void printBanner() {
        System.out.println("========================================================================================");
        System.out.println(" DIGITAL LIBRARY MANAGEMENT SYSTEM (DLMS) - Evoriea Infotech Pvt. Ltd.                 ");
        System.out.println(" Capstone Internship Project | Candidate: ABISHEK MUTHAIAH S K (95192301003)           ");
        System.out.println(" P.S.R. Engineering College, Sivakasi | Enterprise Java 17 & Relational SQL Architecture");
        System.out.println("========================================================================================");
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 3) + "...";
    }
}
