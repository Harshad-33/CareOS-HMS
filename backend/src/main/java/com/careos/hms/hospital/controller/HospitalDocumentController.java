package com.careos.hms.hospital.controller;

import com.careos.hms.common.constants.ApiConstants;
import com.careos.hms.common.response.ApiResponse;
import com.careos.hms.hospital.dto.HospitalDocumentResponse;
import com.careos.hms.hospital.dto.VerifyDocumentRequest;
import com.careos.hms.hospital.entity.Hospital;
import com.careos.hms.hospital.service.HospitalDocumentService;
import com.careos.hms.hospital.service.HospitalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(ApiConstants.API_V1_BASE_PATH)
@Tag(name = "Hospital Documents", description = "Endpoints for compliance document upload, download, and platform verification")
@SecurityRequirement(name = "bearerAuth")
public class HospitalDocumentController {

    private final HospitalDocumentService documentService;
    private final HospitalService hospitalService;

    public HospitalDocumentController(HospitalDocumentService documentService, HospitalService hospitalService) {
        this.documentService = documentService;
        this.hospitalService = hospitalService;
    }

    @GetMapping("/hospitals/me/documents")
    @PreAuthorize("hasAuthority('HOSPITAL_DOCUMENTS_MANAGE')")
    @Operation(summary = "Get compliance documents for current hospital", description = "Hospital administrators can view uploaded compliance documents")
    public ResponseEntity<ApiResponse<List<HospitalDocumentResponse>>> getMyDocuments(HttpServletRequest httpRequest) {
        List<HospitalDocumentResponse> docs = documentService.getMyDocuments();
        return ResponseEntity.ok(ApiResponse.ok(docs, "Documents retrieved successfully", httpRequest.getRequestURI()));
    }

    @PostMapping(value = "/hospitals/me/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('HOSPITAL_DOCUMENTS_MANAGE')")
    @Operation(summary = "Upload compliance document for current hospital", description = "Uploads a PDF, PNG, JPG, or WEBP document up to 10MB")
    public ResponseEntity<ApiResponse<HospitalDocumentResponse>> uploadMyDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "expiryDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        HospitalDocumentResponse response = documentService.uploadMyDocument(documentType, expiryDate, file, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Document uploaded successfully", httpRequest.getRequestURI()));
    }

    @GetMapping("/documents/{documentPublicId}/download")
    @PreAuthorize("hasAnyAuthority('HOSPITAL_DOCUMENTS_MANAGE', 'PLATFORM_DOCUMENTS_VERIFY', 'PLATFORM_HOSPITALS_MANAGE')")
    @Operation(summary = "Download document by public ID", description = "Streams document payload for authorized hospital admin or platform admin")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable String documentPublicId) {
        HospitalDocumentService.DownloadedDocument doc = documentService.downloadDocument(documentPublicId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.filename() + "\"")
                .body(doc.data());
    }

    @PatchMapping("/documents/{documentPublicId}/verify")
    @PreAuthorize("hasAuthority('PLATFORM_DOCUMENTS_VERIFY')")
    @Operation(summary = "Verify or reject compliance document", description = "Platform administrators can verify or reject uploaded hospital documents")
    public ResponseEntity<ApiResponse<HospitalDocumentResponse>> verifyDocument(
            @PathVariable String documentPublicId,
            @Valid @RequestBody VerifyDocumentRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        HospitalDocumentResponse response = documentService.verifyDocument(documentPublicId, request, clientIp);
        return ResponseEntity.ok(ApiResponse.ok(response, "Document verification status updated", httpRequest.getRequestURI()));
    }

    @DeleteMapping("/hospitals/me/documents/{documentPublicId}")
    @PreAuthorize("hasAuthority('HOSPITAL_DOCUMENTS_MANAGE')")
    @Operation(summary = "Delete document for current hospital", description = "Hospital administrators can delete an uploaded document")
    public ResponseEntity<ApiResponse<Void>> deleteMyDocument(
            @PathVariable String documentPublicId,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        documentService.deleteMyDocument(documentPublicId, clientIp);
        return ResponseEntity.ok(ApiResponse.message("Document deleted successfully", httpRequest.getRequestURI()));
    }

    @GetMapping("/hospitals/{hospitalPublicId}/documents")
    @PreAuthorize("hasAuthority('PLATFORM_HOSPITALS_MANAGE')")
    @Operation(summary = "Get documents for hospital by public ID", description = "Platform administrators can view documents belonging to any hospital")
    public ResponseEntity<ApiResponse<List<HospitalDocumentResponse>>> getDocumentsByHospital(
            @PathVariable String hospitalPublicId,
            HttpServletRequest httpRequest
    ) {
        Hospital hospital = hospitalService.getEntityByPublicId(hospitalPublicId);
        List<HospitalDocumentResponse> docs = documentService.getDocuments(hospital.getId());
        return ResponseEntity.ok(ApiResponse.ok(docs, "Documents retrieved successfully", httpRequest.getRequestURI()));
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
