package com.evoriea.dlms.model;

import java.time.LocalDate;

/**
 * Represents a library member in the DLMS registry.
 * Corresponds to the MEMBERS table in 3NF schema.
 */
public class Member {
    private int memberId;
    private String fullName;
    private String email;
    private String phone;
    private MembershipType membershipType;
    private LocalDate joinDate;
    private String status; // ACTIVE, SUSPENDED, EXPIRED

    public Member() {}

    public Member(int memberId, String fullName, String email, String phone, MembershipType membershipType, LocalDate joinDate, String status) {
        this.memberId = memberId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.membershipType = membershipType;
        this.joinDate = joinDate;
        this.status = status;
    }

    public Member(String fullName, String email, String phone, MembershipType membershipType, LocalDate joinDate, String status) {
        this(0, fullName, email, phone, membershipType, joinDate, status);
    }

    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public MembershipType getMembershipType() {
        return membershipType;
    }

    public void setMembershipType(MembershipType membershipType) {
        this.membershipType = membershipType;
    }

    public LocalDate getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(LocalDate joinDate) {
        this.joinDate = joinDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format("[%d] %s (%s) | %s | Tel: %s | Status: %s",
                memberId, fullName, membershipType, email, phone, status);
    }
}
