package com.health.ehr.controller;

import com.health.ehr.dto.ConsentDtos.*;
import com.health.ehr.entity.DoctorProfile;
import com.health.ehr.entity.User;
import com.health.ehr.repository.DoctorProfileRepository;
import com.health.ehr.service.ConsentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/consent")
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentService consentService;
    private final DoctorProfileRepository doctorProfileRepository;

    @PostMapping("/request")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ConsentResponseDto> requestConsent(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody RequestConsentDto dto
    ) {
        DoctorProfile doctor = doctorProfileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Doctor profile not found for user"));
        return ResponseEntity.ok(consentService.requestConsent(doctor.getId(), dto));
    }

    @PostMapping("/verify-otp")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ConsentResponseDto> verifyOtp(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody VerifyOtpDto dto
    ) {
        return ResponseEntity.ok(consentService.verifyOtp(user.getId(), dto));
    }

    @PostMapping("/break-glass")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<BreakGlassResponseDto> triggerBreakGlass(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody BreakGlassRequestDto dto
    ) {
        DoctorProfile doctor = doctorProfileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Doctor profile not found for user"));
        return ResponseEntity.ok(consentService.triggerBreakGlass(doctor.getId(), dto));
    }

    @GetMapping("/doctor")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<ConsentResponseDto>> getDoctorConsents(@AuthenticationPrincipal User user) {
        DoctorProfile doctor = doctorProfileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Doctor profile not found for user"));
        return ResponseEntity.ok(consentService.getDoctorConsents(doctor.getId()));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('PATIENT', 'ADMIN')")
    public ResponseEntity<List<ConsentResponseDto>> getPatientConsents(@PathVariable UUID patientId) {
        return ResponseEntity.ok(consentService.getPatientConsents(patientId));
    }
}
