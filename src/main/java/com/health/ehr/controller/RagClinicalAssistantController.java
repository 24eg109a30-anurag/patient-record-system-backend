package com.health.ehr.controller;

import com.health.ehr.dto.RagDtos.*;
import com.health.ehr.entity.DoctorProfile;
import com.health.ehr.entity.User;
import com.health.ehr.repository.DoctorProfileRepository;
import com.health.ehr.service.RagClinicalAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
public class RagClinicalAssistantController {

    private final RagClinicalAssistantService ragService;
    private final DoctorProfileRepository doctorProfileRepository;

    @PostMapping("/query")
    @PreAuthorize("hasRole('DOCTOR') and @consentSecurity.canAccessPatientRecords(#request.patientId)")
    public ResponseEntity<ClinicalQueryResponse> queryAssistant(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ClinicalQueryRequest request
    ) {
        DoctorProfile doc = doctorProfileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Doctor profile not found"));
        return ResponseEntity.ok(ragService.queryPatientRecords(doc.getId(), request));
    }

    @PostMapping("/ingest")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<String> ingestDocument(
            @Valid @RequestBody DocumentIngestionRequest request
    ) {
        ragService.ingestRecordForRag(
                request.getPatientId(),
                request.getRecordId(),
                request.getDocumentTitle(),
                request.getRawContent()
        );
        return ResponseEntity.ok("Document indexed successfully into vector store.");
    }
}
