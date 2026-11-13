package com.careos.hms.hospital.service;

import com.careos.hms.audit.entity.AuditAction;
import com.careos.hms.audit.service.AuditLogService;
import com.careos.hms.common.exception.ForbiddenOperationException;
import com.careos.hms.common.exception.InvalidRequestException;
import com.careos.hms.common.exception.ResourceNotFoundException;
import com.careos.hms.hospital.dto.HospitalDocumentResponse;
import com.careos.hms.hospital.dto.VerifyDocumentRequest;
import com.careos.hms.hospital.entity.DocumentVerificationStatus;
import com.careos.hms.hospital.entity.Hospital;
import com.careos.hms.hospital.entity.HospitalDocument;
import com.careos.hms.hospital.repository.HospitalDocumentRepository;
import com.careos.hms.hospital.repository.HospitalRepository;
import com.careos.hms.security.CareOsUserPrincipal;
import com.careos.hms.security.TenantContext;
import com.careos.hms.storage.DocumentStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class HospitalDocumentService {

    private final HospitalDocumentRepository documentRepository;
    private final HospitalRepository hospitalRepository;
    private final DocumentStorageService storageService;
    private final AuditLogService auditLogService;
    private final TenantContext tenantContext;

    public HospitalDocumentService(
            HospitalDocumentRepository documentRepository,
            HospitalRepository hospitalRepository,
            DocumentStorageService storageService,
            AuditLogService auditLogService,
            TenantContext tenantContext
    ) {
        this.documentRepository = documentRepository;
        this.hospitalRepository = hospitalRepository;
        this.storageService = storageService;
        this.auditLogService = auditLogService;
        this.tenantContext = tenantContext;
    }

    @Transactional(readOnly = true)
    public List<HospitalDocumentResponse> getDocuments(Long hospitalId) {
        assertHospitalAccess(hospitalId);
        return documentRepository.findByHospitalIdAndDeletedFalse(hospitalId).stream()
                .map(HospitalDocumentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HospitalDocumentResponse> getMyDocuments() {
        return getDocuments(requireCurrentHospitalId());
    }

    @Transactional
    public HospitalDocumentResponse uploadDocument(
            Long hospitalId,
            String documentType,
            LocalDate expiryDate,
            MultipartFile file,
            String clientIp
    ) {
        assertHospitalAccess(hospitalId);
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("A non-empty file must be provided");
        }

        Hospital hospital = hospitalRepository.findByIdAndDeletedFalse(hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital", hospitalId));

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new InvalidRequestException("Failed to read uploaded file payload");
        }

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf";
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        String fileKey = storageService.storeFile(hospital.getPublicId(), originalFilename, bytes, contentType);

        HospitalDocument document = new HospitalDocument(
                hospital,
                documentType != null && !documentType.isBlank() ? documentType.trim() : "OTHER",
                originalFilename,
                fileKey,
                contentType,
                file.getSize(),
                expiryDate
        );

        HospitalDocument saved = documentRepository.saveAndFlush(document);

        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        auditLogService.record(
                principal.getUserId(),
                principal.getUsername(),
                hospitalId,
                AuditAction.DOCUMENT_UPLOADED,
                "HospitalDocument",
                saved.getPublicId(),
                "Uploaded compliance document: " + saved.getDocumentName() + " (" + saved.getDocumentType() + ")",
                null,
                clientIp
        );

        return HospitalDocumentResponse.from(saved);
    }

    @Transactional
    public HospitalDocumentResponse uploadMyDocument(String documentType, LocalDate expiryDate, MultipartFile file, String clientIp) {
        return uploadDocument(requireCurrentHospitalId(), documentType, expiryDate, file, clientIp);
    }

    @Transactional(readOnly = true)
    public DownloadedDocument downloadDocument(String documentPublicId) {
        HospitalDocument document = documentRepository.findByPublicIdAndDeletedFalse(documentPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("HospitalDocument", documentPublicId));

        assertHospitalAccess(document.getHospital().getId());

        byte[] content = storageService.loadFile(document.getFileKey());
        return new DownloadedDocument(document.getDocumentName(), document.getContentType(), content);
    }

    @Transactional
    public HospitalDocumentResponse verifyDocument(String documentPublicId, VerifyDocumentRequest request, String clientIp) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        if (!principal.isPlatformUser()) {
            throw new ForbiddenOperationException("PLATFORM_REQUIRED", "Only platform administrators can verify compliance documents");
        }

        HospitalDocument document = documentRepository.findByPublicIdAndDeletedFalse(documentPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("HospitalDocument", documentPublicId));

        if (request.verificationStatus() == DocumentVerificationStatus.REJECTED
                && (request.rejectionReason() == null || request.rejectionReason().isBlank())) {
            throw new InvalidRequestException("Rejection reason is required when rejecting a document");
        }

        document.setVerificationStatus(request.verificationStatus());
        document.setVerifiedAt(Instant.now());
        document.setVerifiedBy(principal.getUsername());
        document.setRejectionReason(request.verificationStatus() == DocumentVerificationStatus.REJECTED ? request.rejectionReason().trim() : null);

        HospitalDocument saved = documentRepository.saveAndFlush(document);

        AuditAction auditAction = request.verificationStatus() == DocumentVerificationStatus.VERIFIED
                ? AuditAction.DOCUMENT_VERIFIED
                : AuditAction.DOCUMENT_REJECTED;

        auditLogService.record(
                principal.getUserId(),
                principal.getUsername(),
                saved.getHospital().getId(),
                auditAction,
                "HospitalDocument",
                saved.getPublicId(),
                "Document " + request.verificationStatus() + ": " + saved.getDocumentName(),
                null,
                clientIp
        );

        return HospitalDocumentResponse.from(saved);
    }

    @Transactional
    public void deleteDocument(Long hospitalId, String documentPublicId, String clientIp) {
        assertHospitalAccess(hospitalId);
        HospitalDocument document = documentRepository.findByPublicIdAndHospitalIdAndDeletedFalse(documentPublicId, hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("HospitalDocument", documentPublicId));

        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        document.markDeleted(principal.getUsername());
        documentRepository.saveAndFlush(document);

        // Optionally delete file from storage
        storageService.deleteFile(document.getFileKey());

        auditLogService.record(
                principal.getUserId(),
                principal.getUsername(),
                hospitalId,
                AuditAction.DOCUMENT_DELETED,
                "HospitalDocument",
                document.getPublicId(),
                "Deleted document: " + document.getDocumentName(),
                null,
                clientIp
        );
    }

    @Transactional
    public void deleteMyDocument(String documentPublicId, String clientIp) {
        deleteDocument(requireCurrentHospitalId(), documentPublicId, clientIp);
    }

    private Long requireCurrentHospitalId() {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        if (!principal.isHospitalUser() || principal.getHospitalId() == null) {
            throw new ForbiddenOperationException("TENANT_REQUIRED", "Hospital context is required");
        }
        return principal.getHospitalId();
    }

    private void assertHospitalAccess(Long hospitalId) {
        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        if (principal.isPlatformUser()) {
            return;
        }
        if (!Objects.equals(principal.getHospitalId(), hospitalId)) {
            throw new ForbiddenOperationException("FORBIDDEN", "Cannot access documents of another hospital");
        }
    }

    public record DownloadedDocument(String filename, String contentType, byte[] data) {}
}
