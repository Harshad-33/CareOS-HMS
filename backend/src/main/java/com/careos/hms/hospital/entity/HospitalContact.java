package com.careos.hms.hospital.entity;

import com.careos.hms.common.audit.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "hospital_contacts")
public class HospitalContact extends SoftDeletableEntity {

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_type", nullable = false, length = 32)
    private ContactType contactType = ContactType.GENERAL;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "designation", length = 100)
    private String designation;

    @Column(name = "phone_number", nullable = false, length = 32)
    private String phoneNumber;

    @Column(name = "email", length = 160)
    private String email;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;

    protected HospitalContact() {
    }

    public HospitalContact(Hospital hospital, ContactType contactType, String name,
                           String designation, String phoneNumber, String email, boolean primary) {
        this.publicId = UUID.randomUUID().toString();
        this.hospital = hospital;
        this.contactType = contactType;
        this.name = name;
        this.designation = designation;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.primary = primary;
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

    public Hospital getHospital() {
        return hospital;
    }

    public void setHospital(Hospital hospital) {
        this.hospital = hospital;
    }

    public ContactType getContactType() {
        return contactType;
    }

    public void setContactType(ContactType contactType) {
        this.contactType = contactType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }
}
