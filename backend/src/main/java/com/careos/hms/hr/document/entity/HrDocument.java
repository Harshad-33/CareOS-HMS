package com.careos.hms.hr.document.entity;

import com.careos.hms.common.audit.TenantAwareEntity;
import com.careos.hms.employee.entity.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "hr_documents")
public class HrDocument extends TenantAwareEntity {

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private HrDocumentCategory category;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "document_number", length = 64)
    private String documentNumber;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "file_key", nullable = false, length = 255)
    private String fileKey;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private HrDocumentStatus status = HrDocumentStatus.ACTIVE;

    protected HrDocument() {
    }

    public HrDocument(Long hospitalId, Employee employee, HrDocumentCategory category, String title,
                      String fileKey, String originalFilename, String mimeType, long fileSizeBytes) {
        assignHospitalId(hospitalId);
        this.publicId = UUID.randomUUID().toString();
        this.employee = employee;
        this.category = category;
        this.title = title;
        this.fileKey = fileKey;
        this.originalFilename = originalFilename;
        this.mimeType = mimeType;
        this.fileSizeBytes = fileSizeBytes;
        this.status = HrDocumentStatus.ACTIVE;
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

    public Employee getEmployee() {
        return employee;
    }

    public HrDocumentCategory getCategory() {
        return category;
    }

    public void setCategory(HrDocumentCategory category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getFileKey() {
        return fileKey;
    }

    public void setFileKey(String fileKey) {
        this.fileKey = fileKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public HrDocumentStatus getStatus() {
        return status;
    }

    public void setStatus(HrDocumentStatus status) {
        this.status = status;
    }
}
