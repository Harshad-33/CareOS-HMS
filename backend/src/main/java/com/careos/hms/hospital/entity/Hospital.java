package com.careos.hms.hospital.entity;

import com.careos.hms.common.audit.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Hospital tenant entity representing healthcare organizations in CareOS-HMS.
 */
@Entity
@Table(name = "hospitals")
public class Hospital extends SoftDeletableEntity {

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "hospital_code", nullable = false, unique = true, length = 32)
    private String hospitalCode;

    @Column(name = "registration_number", unique = true, length = 64)
    private String registrationNumber;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "hospital_type", nullable = false, length = 32)
    private HospitalType hospitalType = HospitalType.GENERAL;

    @Column(name = "email", length = 160)
    private String email;

    @Column(name = "primary_contact_number", length = 32)
    private String primaryContactNumber;

    @Column(name = "alternate_contact_number", length = 32)
    private String alternateContactNumber;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "address_line_1", length = 255)
    private String addressLine1;

    @Column(name = "address_line_2", length = 255)
    private String addressLine2;

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

    @Column(name = "established_date")
    private LocalDate establishedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private HospitalStatus status = HospitalStatus.ACTIVE;

    protected Hospital() {
    }

    public Hospital(String hospitalCode, String name) {
        this.publicId = UUID.randomUUID().toString();
        this.hospitalCode = hospitalCode;
        this.name = name;
        this.status = HospitalStatus.ACTIVE;
        this.hospitalType = HospitalType.GENERAL;
        this.country = "India";
    }

    @PrePersist
    void ensurePublicId() {
        if (this.publicId == null || this.publicId.isBlank()) {
            this.publicId = UUID.randomUUID().toString();
        }
    }

    public String getPublicId() {
        return publicId;
    }

    public String getHospitalCode() {
        return hospitalCode;
    }

    public void setHospitalCode(String hospitalCode) {
        this.hospitalCode = hospitalCode;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HospitalType getHospitalType() {
        return hospitalType;
    }

    public void setHospitalType(HospitalType hospitalType) {
        this.hospitalType = hospitalType;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPrimaryContactNumber() {
        return primaryContactNumber;
    }

    public void setPrimaryContactNumber(String primaryContactNumber) {
        this.primaryContactNumber = primaryContactNumber;
    }

    public String getAlternateContactNumber() {
        return alternateContactNumber;
    }

    public void setAlternateContactNumber(String alternateContactNumber) {
        this.alternateContactNumber = alternateContactNumber;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
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

    public LocalDate getEstablishedDate() {
        return establishedDate;
    }

    public void setEstablishedDate(LocalDate establishedDate) {
        this.establishedDate = establishedDate;
    }

    public HospitalStatus getStatus() {
        return status;
    }

    public void setStatus(HospitalStatus status) {
        this.status = status;
    }
}
