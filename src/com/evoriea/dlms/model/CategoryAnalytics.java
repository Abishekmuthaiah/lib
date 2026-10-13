package com.evoriea.dlms.model;

/**
 * Data model for Category-wise circulation and fine collection analytics.
 * Corresponds to Slide 11 management insights.
 */
public class CategoryAnalytics {
    private String category;
    private int booksIssued;
    private double fineCollected;
    private double shareOfIssues;

    public CategoryAnalytics() {}

    public CategoryAnalytics(String category, int booksIssued, double fineCollected, double shareOfIssues) {
        this.category = category;
        this.booksIssued = booksIssued;
        this.fineCollected = fineCollected;
        this.shareOfIssues = shareOfIssues;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getBooksIssued() {
        return booksIssued;
    }

    public void setBooksIssued(int booksIssued) {
        this.booksIssued = booksIssued;
    }

    public double getFineCollected() {
        return fineCollected;
    }

    public void setFineCollected(double fineCollected) {
        this.fineCollected = fineCollected;
    }

    public double getShareOfIssues() {
        return shareOfIssues;
    }

    public void setShareOfIssues(double shareOfIssues) {
        this.shareOfIssues = shareOfIssues;
    }

    @Override
    public String toString() {
        return String.format("%-25s | %12d | Rs. %8.2f | %6.1f%%",
                category, booksIssued, fineCollected, shareOfIssues);
    }
}
