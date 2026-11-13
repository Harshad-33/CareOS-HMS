package com.careos.hms.hospital.service;

import com.careos.hms.audit.entity.AuditAction;
import com.careos.hms.audit.service.AuditLogService;
import com.careos.hms.common.exception.ForbiddenOperationException;
import com.careos.hms.common.exception.ResourceNotFoundException;
import com.careos.hms.hospital.dto.HospitalContactRequest;
import com.careos.hms.hospital.dto.HospitalContactResponse;
import com.careos.hms.hospital.entity.Hospital;
import com.careos.hms.hospital.entity.HospitalContact;
import com.careos.hms.hospital.repository.HospitalContactRepository;
import com.careos.hms.hospital.repository.HospitalRepository;
import com.careos.hms.security.CareOsUserPrincipal;
import com.careos.hms.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class HospitalContactService {

    private final HospitalContactRepository contactRepository;
    private final HospitalRepository hospitalRepository;
    private final AuditLogService auditLogService;
    private final TenantContext tenantContext;

    public HospitalContactService(
            HospitalContactRepository contactRepository,
            HospitalRepository hospitalRepository,
            AuditLogService auditLogService,
            TenantContext tenantContext
    ) {
        this.contactRepository = contactRepository;
        this.hospitalRepository = hospitalRepository;
        this.auditLogService = auditLogService;
        this.tenantContext = tenantContext;
    }

    @Transactional(readOnly = true)
    public List<HospitalContactResponse> getContacts(Long hospitalId) {
        assertHospitalAccess(hospitalId);
        return contactRepository.findByHospitalIdAndDeletedFalse(hospitalId).stream()
                .map(HospitalContactResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HospitalContactResponse> getMyContacts() {
        return getContacts(requireCurrentHospitalId());
    }

    @Transactional
    public HospitalContactResponse createContact(Long hospitalId, HospitalContactRequest request, String clientIp) {
        assertHospitalAccess(hospitalId);
        Hospital hospital = hospitalRepository.findByIdAndDeletedFalse(hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital", hospitalId));

        if (request.primary()) {
            unmarkPrimaryContacts(hospitalId);
        }

        HospitalContact contact = new HospitalContact(
                hospital,
                request.contactType(),
                request.name().trim(),
                request.designation() != null ? request.designation().trim() : null,
                request.phoneNumber().trim(),
                request.email() != null ? request.email().trim().toLowerCase(Locale.ROOT) : null,
                request.primary()
        );

        HospitalContact saved = contactRepository.saveAndFlush(contact);

        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        auditLogService.record(
                principal.getUserId(),
                principal.getUsername(),
                hospitalId,
                AuditAction.CONTACT_CREATED,
                "HospitalContact",
                saved.getPublicId(),
                "Added contact: " + saved.getName() + " (" + saved.getContactType() + ")",
                null,
                clientIp
        );

        return HospitalContactResponse.from(saved);
    }

    @Transactional
    public HospitalContactResponse createMyContact(HospitalContactRequest request, String clientIp) {
        return createContact(requireCurrentHospitalId(), request, clientIp);
    }

    @Transactional
    public HospitalContactResponse updateContact(Long hospitalId, String publicId, HospitalContactRequest request, String clientIp) {
        assertHospitalAccess(hospitalId);
        HospitalContact contact = contactRepository.findByPublicIdAndHospitalIdAndDeletedFalse(publicId, hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("HospitalContact", publicId));

        if (request.primary() && !contact.isPrimary()) {
            unmarkPrimaryContacts(hospitalId);
        }

        contact.setContactType(request.contactType());
        contact.setName(request.name().trim());
        contact.setDesignation(request.designation() != null ? request.designation().trim() : null);
        contact.setPhoneNumber(request.phoneNumber().trim());
        contact.setEmail(request.email() != null ? request.email().trim().toLowerCase(Locale.ROOT) : null);
        contact.setPrimary(request.primary());

        HospitalContact saved = contactRepository.saveAndFlush(contact);

        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        auditLogService.record(
                principal.getUserId(),
                principal.getUsername(),
                hospitalId,
                AuditAction.CONTACT_UPDATED,
                "HospitalContact",
                saved.getPublicId(),
                "Updated contact: " + saved.getName(),
                null,
                clientIp
        );

        return HospitalContactResponse.from(saved);
    }

    @Transactional
    public HospitalContactResponse updateMyContact(String publicId, HospitalContactRequest request, String clientIp) {
        return updateContact(requireCurrentHospitalId(), publicId, request, clientIp);
    }

    @Transactional
    public void deleteContact(Long hospitalId, String publicId, String clientIp) {
        assertHospitalAccess(hospitalId);
        HospitalContact contact = contactRepository.findByPublicIdAndHospitalIdAndDeletedFalse(publicId, hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("HospitalContact", publicId));

        CareOsUserPrincipal principal = tenantContext.getRequiredPrincipal();
        contact.markDeleted(principal.getUsername());
        contactRepository.saveAndFlush(contact);

        auditLogService.record(
                principal.getUserId(),
                principal.getUsername(),
                hospitalId,
                AuditAction.CONTACT_DELETED,
                "HospitalContact",
                contact.getPublicId(),
                "Deleted contact: " + contact.getName(),
                null,
                clientIp
        );
    }

    @Transactional
    public void deleteMyContact(String publicId, String clientIp) {
        deleteContact(requireCurrentHospitalId(), publicId, clientIp);
    }

    private void unmarkPrimaryContacts(Long hospitalId) {
        List<HospitalContact> contacts = contactRepository.findByHospitalIdAndDeletedFalse(hospitalId);
        for (HospitalContact c : contacts) {
            if (c.isPrimary()) {
                c.setPrimary(false);
                contactRepository.save(c);
            }
        }
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
            throw new ForbiddenOperationException("FORBIDDEN", "Cannot access contacts of another hospital");
        }
    }
}
