package com.health.ehr.controller;

import com.health.ehr.dto.EhrDtos.*;
import com.health.ehr.entity.DoctorProfile;
import com.health.ehr.entity.Role;
import com.health.ehr.entity.User;
import com.health.ehr.repository.DoctorProfileRepository;
import com.health.ehr.service.MedicalRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;
    private final DoctorProfileRepository doctorProfileRepository;

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("@consentSecurity.canAccessPatientRecords(#patientId)")
    public ResponseEntity<List<MedicalRecordTimelineDto>> getPatientTimeline(
            @PathVariable UUID patientId,
            @AuthenticationPrincipal User user
    ) {
        UUID doctorId = null;
        if (user.getRole() == Role.DOCTOR) {
            DoctorProfile doc = doctorProfileRepository.findByUser(user).orElse(null);
            if (doc != null) doctorId = doc.getId();
        }
        return ResponseEntity.ok(medicalRecordService.getPatientTimeline(patientId, doctorId));
    }

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR') and @consentSecurity.canAccessPatientRecords(#dto.patientId)")
    public ResponseEntity<MedicalRecordTimelineDto> createEncounter(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateMedicalRecordDto dto
    ) {
        DoctorProfile doc = doctorProfileRepository.findByUser(user).orElse(null);
        UUID doctorId = doc != null ? doc.getId() : null;
        return ResponseEntity.ok(medicalRecordService.createEncounter(doctorId, dto));
    }
}
