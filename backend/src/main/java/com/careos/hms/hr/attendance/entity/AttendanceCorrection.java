package com.careos.hms.hr.attendance.entity;

import com.careos.hms.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "attendance_corrections")
public class AttendanceCorrection extends BaseEntity {

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "hospital_id", nullable = false)
    private Long hospitalId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attendance_id", nullable = false)
    private Attendance attendance;

    @Column(name = "requested_check_in", nullable = false)
    private LocalTime requestedCheckIn;

    @Column(name = "requested_check_out", nullable = false)
    private LocalTime requestedCheckOut;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private CorrectionStatus status = CorrectionStatus.PENDING;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "reviewer_username", length = 64)
    private String reviewerUsername;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    protected AttendanceCorrection() {
    }

    public AttendanceCorrection(Long hospitalId, Attendance attendance, LocalTime requestedCheckIn,
                                LocalTime requestedCheckOut, String reason) {
        this.publicId = UUID.randomUUID().toString();
        this.hospitalId = hospitalId;
        this.attendance = attendance;
        this.requestedCheckIn = requestedCheckIn;
        this.requestedCheckOut = requestedCheckOut;
        this.reason = reason;
        this.status = CorrectionStatus.PENDING;
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

    public Long getHospitalId() {
        return hospitalId;
    }

    public Attendance getAttendance() {
        return attendance;
    }

    public LocalTime getRequestedCheckIn() {
        return requestedCheckIn;
    }

    public void setRequestedCheckIn(LocalTime requestedCheckIn) {
        this.requestedCheckIn = requestedCheckIn;
    }

    public LocalTime getRequestedCheckOut() {
        return requestedCheckOut;
    }

    public void setRequestedCheckOut(LocalTime requestedCheckOut) {
        this.requestedCheckOut = requestedCheckOut;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public CorrectionStatus getStatus() {
        return status;
    }

    public void setStatus(CorrectionStatus status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getReviewerUsername() {
        return reviewerUsername;
    }

    public void setReviewerUsername(String reviewerUsername) {
        this.reviewerUsername = reviewerUsername;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
