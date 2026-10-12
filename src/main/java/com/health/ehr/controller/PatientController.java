package com.health.ehr.controller;

import com.health.ehr.dto.EhrDtos.PatientSummaryDto;
import com.health.ehr.entity.DoctorProfile;
import com.health.ehr.entity.PatientProfile;
import com.health.ehr.entity.Role;
import com.health.ehr.entity.User;
import com.health.ehr.repository.DoctorProfileRepository;
import com.health.ehr.repository.PatientProfileRepository;
import com.health.ehr.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final MedicalRecordService medicalRecordService;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<List<PatientSummaryDto>> searchPatients(
            @RequestParam(required = false) String query,
            @AuthenticationPrincipal User user
    ) {
        UUID doctorId = null;
        if (user.getRole() == Role.DOCTOR) {
            DoctorProfile doc = doctorProfileRepository.findByUser(user).orElse(null);
            if (doc != null) doctorId = doc.getId();
        }
        return ResponseEntity.ok(medicalRecordService.searchPatients(query, doctorId));
    }

    @GetMapping("/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'AUDITOR') or (hasRole('PATIENT') and @consentSecurity.canAccessPatientRecords(#patientId))")
    public ResponseEntity<PatientSummaryDto> getPatientSummary(
            @PathVariable UUID patientId,
            @AuthenticationPrincipal User user
    ) {
        UUID doctorId = null;
        if (user.getRole() == Role.DOCTOR) {
            DoctorProfile doc = doctorProfileRepository.findByUser(user).orElse(null);
            if (doc != null) doctorId = doc.getId();
        }
        return ResponseEntity.ok(medicalRecordService.getPatientSummary(patientId, doctorId));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PatientProfile> getCurrentPatient(@AuthenticationPrincipal User user) {
        return patientProfileRepository.findByUser(user)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
