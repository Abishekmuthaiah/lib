package com.evoriea.dlms.model;

/**
 * Membership tiers and their associated borrowing policies.
 * 
 * Policies (Slide 8):
 * - STUDENT: 14 days loan period, 3 concurrent books limit
 * - FACULTY: 30 days loan period, 5 concurrent books limit
 * - GENERAL: 7 days loan period, 2 concurrent books limit
 */
public enum MembershipType {
    STUDENT(14, 3, "Student"),
    FACULTY(30, 5, "Faculty"),
    GENERAL(7, 2, "General Member");

    private final int loanPeriodDays;
    private final int borrowingLimit;
    private final String displayName;

    MembershipType(int loanPeriodDays, int borrowingLimit, String displayName) {
        this.loanPeriodDays = loanPeriodDays;
        this.borrowingLimit = borrowingLimit;
        this.displayName = displayName;
    }

    public int getLoanPeriodDays() {
        return loanPeriodDays;
    }

    public int getBorrowingLimit() {
        return borrowingLimit;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static MembershipType fromString(String type) {
        if (type == null) {
            return GENERAL;
        }
        for (MembershipType m : values()) {
            if (m.name().equalsIgnoreCase(type.trim())) {
                return m;
            }
        }
        return GENERAL;
    }
}
