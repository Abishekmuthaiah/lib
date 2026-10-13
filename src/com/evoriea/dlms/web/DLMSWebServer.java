package com.evoriea.dlms.web;

import com.evoriea.dlms.config.DBConnection;
import com.evoriea.dlms.dao.LibraryDAO;
import com.evoriea.dlms.data.DatabaseSeeder;
import com.evoriea.dlms.model.Book;
import com.evoriea.dlms.model.BookIssue;
import com.evoriea.dlms.model.CategoryAnalytics;
import com.evoriea.dlms.model.Fine;
import com.evoriea.dlms.model.Member;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Built-in Lightweight HTTP Server & REST API for DLMS Dashboard.
 * Uses Java standard library com.sun.net.httpserver (zero external dependencies).
 */
public class DLMSWebServer {

    private final int port;
    private final LibraryDAO dao;
    private HttpServer server;

    public DLMSWebServer(int port) {
        this.port = port;
        this.dao = new LibraryDAO();
    }

    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        // Initialize DAO and Database
        LibraryDAO dao = new LibraryDAO();
        dao.initializeSchema();
        DatabaseSeeder seeder = new DatabaseSeeder(dao);
        seeder.seedIfEmpty();

        DLMSWebServer webServer = new DLMSWebServer(port);
        webServer.start();

        System.out.println("==========================================================================");
        System.out.println(" DIGITAL LIBRARY MANAGEMENT SYSTEM - WEB DASHBOARD");
        System.out.println(" Server running on http://localhost:" + port);
        System.out.println(" Press Ctrl+C in terminal to stop server.");
        System.out.println("==========================================================================");
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/catalog", new CatalogHandler());
        server.createContext("/api/members", new MembersHandler());
        server.createContext("/api/issues", new IssuesHandler());
        server.createContext("/api/fines", new FinesHandler());
        server.createContext("/api/analytics", new AnalyticsHandler());
        server.createContext("/api/stats", new StatsHandler());
        server.createContext("/api/issue", new IssueActionHandler());
        server.createContext("/api/return", new ReturnActionHandler());
        server.createContext("/api/pay", new PayFineHandler());
        server.createContext("/api/reseed", new ReseedHandler());

        server.setExecutor(null);
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // --- Handlers ---

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if ("/".equals(path) || path.isEmpty()) {
                path = "/index.html";
            }

            File file = new File("web" + path);
            if (!file.exists() || file.isDirectory()) {
                sendResponse(exchange, 404, "text/plain", "404 Not Found");
                return;
            }

            String contentType = "text/plain";
            if (path.endsWith(".html")) contentType = "text/html; charset=utf-8";
            else if (path.endsWith(".css")) contentType = "text/css; charset=utf-8";
            else if (path.endsWith(".js")) contentType = "application/javascript; charset=utf-8";
            else if (path.endsWith(".json")) contentType = "application/json";
            else if (path.endsWith(".svg")) contentType = "image/svg+xml";

            byte[] bytes = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(bytes);
            }

            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private class CatalogHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                List<Book> books = dao.getAllBooks();
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < books.size(); i++) {
                    Book b = books.get(i);
                    json.append(String.format("{\"bookId\":%d,\"isbn\":\"%s\",\"title\":\"%s\",\"author\":\"%s\",\"publisher\":\"%s\",\"category\":\"%s\",\"totalCopies\":%d,\"availableCopies\":%d}",
                            b.getBookId(), escape(b.getIsbn()), escape(b.getTitle()), escape(b.getAuthor()),
                            escape(b.getPublisher()), escape(b.getCategory()), b.getTotalCopies(), b.getAvailableCopies()));
                    if (i < books.size() - 1) json.append(",");
                }
                json.append("]");
                sendResponse(exchange, 200, "application/json", json.toString());
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json", "{\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        }
    }

    private class MembersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                List<Member> members = dao.getAllMembers();
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < members.size(); i++) {
                    Member m = members.get(i);
                    int activeLoans = dao.getActiveLoanCount(m.getMemberId());
                    json.append(String.format("{\"memberId\":%d,\"fullName\":\"%s\",\"email\":\"%s\",\"phone\":\"%s\",\"membershipType\":\"%s\",\"joinDate\":\"%s\",\"status\":\"%s\",\"activeLoans\":%d,\"maxLimit\":%d}",
                            m.getMemberId(), escape(m.getFullName()), escape(m.getEmail()), escape(m.getPhone()),
                            m.getMembershipType().name(), m.getJoinDate(), m.getStatus(), activeLoans, m.getMembershipType().getBorrowingLimit()));
                    if (i < members.size() - 1) json.append(",");
                }
                json.append("]");
                sendResponse(exchange, 200, "application/json", json.toString());
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json", "{\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        }
    }

    private class IssuesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                List<BookIssue> issues = dao.getAllIssues();
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < issues.size(); i++) {
                    BookIssue bi = issues.get(i);
                    json.append(String.format("{\"issueId\":%d,\"bookId\":%d,\"memberId\":%d,\"bookTitle\":\"%s\",\"isbn\":\"%s\",\"memberName\":\"%s\",\"membershipType\":\"%s\",\"issueDate\":\"%s\",\"dueDate\":\"%s\",\"returnDate\":\"%s\",\"status\":\"%s\"}",
                            bi.getIssueId(), bi.getBookId(), bi.getMemberId(), escape(bi.getBookTitle()), escape(bi.getIsbn()),
                            escape(bi.getMemberName()), bi.getMembershipType().name(), bi.getIssueDate(), bi.getDueDate(),
                            bi.getReturnDate() != null ? bi.getReturnDate().toString() : "", bi.getStatus()));
                    if (i < issues.size() - 1) json.append(",");
                }
                json.append("]");
                sendResponse(exchange, 200, "application/json", json.toString());
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json", "{\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        }
    }

    private class FinesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                List<Fine> fines = dao.getAllFines();
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < fines.size(); i++) {
                    Fine f = fines.get(i);
                    json.append(String.format("{\"fineId\":%d,\"issueId\":%d,\"memberId\":%d,\"memberName\":\"%s\",\"bookTitle\":\"%s\",\"amount\":%.2f,\"reason\":\"%s\",\"paidStatus\":\"%s\",\"overdueDays\":%d}",
                            f.getFineId(), f.getIssueId(), f.getMemberId(), escape(f.getMemberName()), escape(f.getBookTitle()),
                            f.getAmount(), escape(f.getReason()), f.getPaidStatus(), f.getOverdueDays()));
                    if (i < fines.size() - 1) json.append(",");
                }
                json.append("]");
                sendResponse(exchange, 200, "application/json", json.toString());
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json", "{\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        }
    }

    private class AnalyticsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                List<CategoryAnalytics> analytics = dao.getCategoryAnalytics();
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < analytics.size(); i++) {
                    CategoryAnalytics ca = analytics.get(i);
                    json.append(String.format("{\"category\":\"%s\",\"booksIssued\":%d,\"fineCollected\":%.2f,\"shareOfIssues\":%.1f}",
                            escape(ca.getCategory()), ca.getBooksIssued(), ca.getFineCollected(), ca.getShareOfIssues()));
                    if (i < analytics.size() - 1) json.append(",");
                }
                json.append("]");
                sendResponse(exchange, 200, "application/json", json.toString());
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json", "{\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        }
    }

    private class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                int books = dao.getBookCount();
                int members = dao.getMemberCount();
                int issues = dao.getIssueCount();
                int activeLoans = dao.getActiveIssues().size();
                List<Fine> fines = dao.getAllFines();
                double totalFines = 0;
                double collectedFines = 0;
                for (Fine f : fines) {
                    totalFines += f.getAmount();
                    if ("PAID".equalsIgnoreCase(f.getPaidStatus())) collectedFines += f.getAmount();
                }

                String json = String.format("{\"books\":%d,\"members\":%d,\"totalIssues\":%d,\"activeLoans\":%d,\"totalFines\":%.2f,\"collectedFines\":%.2f}",
                        books, members, issues, activeLoans, totalFines, collectedFines);
                sendResponse(exchange, 200, "application/json", json);
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json", "{\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        }
    }

    private class IssueActionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
                return;
            }
            try {
                String body = readBody(exchange);
                Map<String, String> params = parseFormBody(body);
                int memberId = Integer.parseInt(params.get("memberId"));
                String bookInput = params.get("bookId");
                int bookId = 0;
                try {
                    bookId = Integer.parseInt(bookInput);
                } catch (NumberFormatException e) {
                    Book b = dao.getBookByIsbn(bookInput);
                    if (b != null) bookId = b.getBookId();
                }

                BookIssue issue = dao.issueBook(memberId, bookId, LocalDate.now());
                String json = String.format("{\"success\":true,\"message\":\"Book issued successfully\",\"issueId\":%d,\"dueDate\":\"%s\"}",
                        issue.getIssueId(), issue.getDueDate());
                sendResponse(exchange, 200, "application/json", json);
            } catch (Exception e) {
                sendResponse(exchange, 400, "application/json", "{\"success\":false,\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        }
    }

    private class ReturnActionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
                return;
            }
            try {
                String body = readBody(exchange);
                Map<String, String> params = parseFormBody(body);
                int issueId = Integer.parseInt(params.get("issueId"));
                LocalDate returnDate = LocalDate.now();
                if (params.containsKey("returnDate") && !params.get("returnDate").isEmpty()) {
                    returnDate = LocalDate.parse(params.get("returnDate"));
                }

                Fine fine = dao.returnBook(issueId, returnDate);
                String receipt = fine != null ? fine.toFormattedReceipt() : "";
                double fineAmt = fine != null ? fine.getAmount() : 0.0;
                String json = String.format("{\"success\":true,\"message\":\"Return processed\",\"fineAmount\":%.2f,\"receipt\":\"%s\"}",
                        fineAmt, escape(receipt));
                sendResponse(exchange, 200, "application/json", json);
            } catch (Exception e) {
                sendResponse(exchange, 400, "application/json", "{\"success\":false,\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        }
    }

    private class PayFineHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String body = readBody(exchange);
                Map<String, String> params = parseFormBody(body);
                int fineId = Integer.parseInt(params.get("fineId"));
                boolean ok = dao.markFineAsPaid(fineId);
                sendResponse(exchange, 200, "application/json", "{\"success\":" + ok + "}");
            } catch (Exception e) {
                sendResponse(exchange, 400, "application/json", "{\"success\":false,\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        }
    }

    private class ReseedHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                DatabaseSeeder seeder = new DatabaseSeeder(dao);
                seeder.seedAll();
                sendResponse(exchange, 200, "application/json", "{\"success\":true,\"message\":\"Database successfully reseeded!\"}");
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json", "{\"success\":false,\"error\":\"" + escape(e.getMessage()) + "\"}");
            }
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Map<String, String> parseFormBody(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.isEmpty()) return map;

        if (body.startsWith("{") && body.endsWith("}")) {
            // Simple JSON parsing
            String trimmed = body.substring(1, body.length() - 1);
            String[] pairs = trimmed.split(",");
            for (String p : pairs) {
                String[] kv = p.split(":", 2);
                if (kv.length == 2) {
                    String k = kv[0].trim().replace("\"", "");
                    String v = kv[1].trim().replace("\"", "");
                    map.put(k, v);
                }
            }
        } else {
            // URL-encoded form data
            String[] pairs = body.split("&");
            for (String p : pairs) {
                String[] kv = p.split("=", 2);
                if (kv.length == 2) {
                    map.put(kv[0], java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
                }
            }
        }
        return map;
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
