package com.careos.hms.branch.entity;

import com.careos.hms.common.audit.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "hospital_branches")
public class HospitalBranch extends TenantAwareEntity {

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "branch_code", nullable = false, length = 32)
    private String branchCode;

    @Column(name = "branch_name", nullable = false, length = 100)
    private String branchName;

    @Column(name = "contact_number", length = 32)
    private String contactNumber;

    @Column(name = "email", length = 160)
    private String email;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "country", nullable = false, length = 100)
    private String country = "India";

    @Column(name = "pin_code", length = 20)
    private String pinCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private BranchStatus status = BranchStatus.ACTIVE;

    protected HospitalBranch() {
    }

    public HospitalBranch(Long hospitalId, String branchCode, String branchName) {
        assignHospitalId(hospitalId);
        this.publicId = UUID.randomUUID().toString();
        this.branchCode = branchCode;
        this.branchName = branchName;
        this.status = BranchStatus.ACTIVE;
        this.country = "India";
    }

    @PrePersist
    void ensurePublicId() {
        if (this.publicId == null || this.publicId.isBlank()) {
            this.publicId = UUID.randomUUID().toString();
        }
    }

    public void setHospitalId(Long hospitalId) {
        assignHospitalId(hospitalId);
    }

    public String getPublicId() {
        return publicId;
    }

    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPinCode() {
        return pinCode;
    }

    public void setPinCode(String pinCode) {
        this.pinCode = pinCode;
    }

    public BranchStatus getStatus() {
        return status;
    }

    public void setStatus(BranchStatus status) {
        this.status = status;
    }
}
