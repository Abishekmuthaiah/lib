package com.evoriea.dlms.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents an Overdue Fine assessment record in the DLMS audit trail.
 * Corresponds to the FINES table in 3NF schema.
 * Overdue rate: Rs. 5.00 / day (Slide 8 & 10)
 */
public class Fine {
    public static final double FINE_RATE_PER_DAY = 5.00;

    private int fineId;
    private int issueId;
    private int memberId;
    private double amount;
    private String reason;
    private String paidStatus; // UNPAID, PAID
    private LocalDateTime createdAt;

    // Joined metadata for formatted receipt printing (Slide 10)
    private String memberName;
    private MembershipType membershipType;
    private String bookTitle;
    private String isbn;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private long overdueDays;

    public Fine() {}

    public Fine(int fineId, int issueId, int memberId, double amount, String reason, String paidStatus, LocalDateTime createdAt) {
        this.fineId = fineId;
        this.issueId = issueId;
        this.memberId = memberId;
        this.amount = amount;
        this.reason = reason;
        this.paidStatus = paidStatus;
        this.createdAt = createdAt;
    }

    public int getFineId() {
        return fineId;
    }

    public void setFineId(int fineId) {
        this.fineId = fineId;
    }

    public int getIssueId() {
        return issueId;
    }

    public void setIssueId(int issueId) {
        this.issueId = issueId;
    }

    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getPaidStatus() {
        return paidStatus;
    }

    public void setPaidStatus(String paidStatus) {
        this.paidStatus = paidStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public MembershipType getMembershipType() {
        return membershipType;
    }

    public void setMembershipType(MembershipType membershipType) {
        this.membershipType = membershipType;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public long getOverdueDays() {
        return overdueDays;
    }

    public void setOverdueDays(long overdueDays) {
        this.overdueDays = overdueDays;
    }

    /**
     * Formats receipt exactly as specified in Slide 10.
     */
    public String toFormattedReceipt() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=================================================================\n");
        sb.append(String.format("         SAMPLE FINE RECEIPT (ISSUE ID %d)\n", issueId));
        sb.append("=================================================================\n");
        sb.append(String.format(" * Issue ID / Member : %d | %s (%s)\n",
                issueId,
                memberName != null ? memberName : "Member " + memberId,
                membershipType != null ? membershipType.name() : "GENERAL"));
        sb.append(String.format(" * Book Title / ISBN : %s / %s\n",
                bookTitle != null ? bookTitle : "Unknown Book",
                isbn != null ? isbn : "N/A"));
        sb.append(String.format(" * Issue Date        : %s\n", issueDate != null ? issueDate.toString() : "N/A"));
        sb.append(String.format(" * Due Date          : %s\n", dueDate != null ? dueDate.toString() : "N/A"));
        sb.append(String.format(" * Return Date       : %s\n", returnDate != null ? returnDate.toString() : "N/A"));
        sb.append(String.format(" * Overdue Days      : %d\n", overdueDays));
        sb.append(String.format(" * Fine Rate         : Rs. %.2f / day\n", FINE_RATE_PER_DAY));
        sb.append(String.format(" * FINE PAYABLE      : Rs. %.2f (%d x Rs. %.2f)\n", amount, overdueDays, FINE_RATE_PER_DAY));
        sb.append(String.format(" * Payment Status    : %s\n", paidStatus));
        sb.append("=================================================================\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("Fine #%d [Issue #%d, Member #%d]: Rs. %.2f (%s) - %s",
                fineId, issueId, memberId, amount, paidStatus, reason);
    }
}
