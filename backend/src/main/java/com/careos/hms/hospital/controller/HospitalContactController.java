package com.careos.hms.hospital.controller;

import com.careos.hms.common.constants.ApiConstants;
import com.careos.hms.common.response.ApiResponse;
import com.careos.hms.hospital.dto.HospitalContactRequest;
import com.careos.hms.hospital.dto.HospitalContactResponse;
import com.careos.hms.hospital.entity.Hospital;
import com.careos.hms.hospital.service.HospitalContactService;
import com.careos.hms.hospital.service.HospitalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiConstants.API_V1_BASE_PATH + "/hospitals")
@Tag(name = "Hospital Contacts", description = "Endpoints for managing hospital key personnel and department contacts")
@SecurityRequirement(name = "bearerAuth")
public class HospitalContactController {

    private final HospitalContactService contactService;
    private final HospitalService hospitalService;

    public HospitalContactController(HospitalContactService contactService, HospitalService hospitalService) {
        this.contactService = contactService;
        this.hospitalService = hospitalService;
    }

    @GetMapping("/me/contacts")
    @PreAuthorize("hasAuthority('HOSPITAL_CONTACTS_MANAGE')")
    @Operation(summary = "Get contacts for current hospital", description = "Hospital administrators can view all contacts for their hospital")
    public ResponseEntity<ApiResponse<List<HospitalContactResponse>>> getMyContacts(HttpServletRequest httpRequest) {
        List<HospitalContactResponse> contacts = contactService.getMyContacts();
        return ResponseEntity.ok(ApiResponse.ok(contacts, "Contacts retrieved successfully", httpRequest.getRequestURI()));
    }

    @PostMapping("/me/contacts")
    @PreAuthorize("hasAuthority('HOSPITAL_CONTACTS_MANAGE')")
    @Operation(summary = "Create contact for current hospital", description = "Hospital administrators can add key contacts for their hospital")
    public ResponseEntity<ApiResponse<HospitalContactResponse>> createMyContact(
            @Valid @RequestBody HospitalContactRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        HospitalContactResponse response = contactService.createMyContact(request, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Contact created successfully", httpRequest.getRequestURI()));
    }

    @PutMapping("/me/contacts/{contactPublicId}")
    @PreAuthorize("hasAuthority('HOSPITAL_CONTACTS_MANAGE')")
    @Operation(summary = "Update contact for current hospital", description = "Hospital administrators can update an existing contact")
    public ResponseEntity<ApiResponse<HospitalContactResponse>> updateMyContact(
            @PathVariable String contactPublicId,
            @Valid @RequestBody HospitalContactRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        HospitalContactResponse response = contactService.updateMyContact(contactPublicId, request, clientIp);
        return ResponseEntity.ok(ApiResponse.ok(response, "Contact updated successfully", httpRequest.getRequestURI()));
    }

    @DeleteMapping("/me/contacts/{contactPublicId}")
    @PreAuthorize("hasAuthority('HOSPITAL_CONTACTS_MANAGE')")
    @Operation(summary = "Delete contact for current hospital", description = "Hospital administrators can soft-delete a contact")
    public ResponseEntity<ApiResponse<Void>> deleteMyContact(
            @PathVariable String contactPublicId,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        contactService.deleteMyContact(contactPublicId, clientIp);
        return ResponseEntity.ok(ApiResponse.message("Contact deleted successfully", httpRequest.getRequestURI()));
    }

    @GetMapping("/{hospitalPublicId}/contacts")
    @PreAuthorize("hasAuthority('PLATFORM_HOSPITALS_MANAGE')")
    @Operation(summary = "Get contacts by hospital public ID", description = "Platform administrators can view contacts of any hospital")
    public ResponseEntity<ApiResponse<List<HospitalContactResponse>>> getContactsByHospital(
            @PathVariable String hospitalPublicId,
            HttpServletRequest httpRequest
    ) {
        Hospital hospital = hospitalService.getEntityByPublicId(hospitalPublicId);
        List<HospitalContactResponse> contacts = contactService.getContacts(hospital.getId());
        return ResponseEntity.ok(ApiResponse.ok(contacts, "Contacts retrieved successfully", httpRequest.getRequestURI()));
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
