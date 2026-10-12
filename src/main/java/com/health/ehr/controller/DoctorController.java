package com.health.ehr.controller;

import com.health.ehr.entity.DoctorProfile;
import com.health.ehr.entity.User;
import com.health.ehr.repository.DoctorProfileRepository;
import com.health.ehr.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorProfileRepository doctorProfileRepository;
    private final AuthService authService;

    @GetMapping("/pending-verification")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DoctorProfile>> getPendingDoctors() {
        return ResponseEntity.ok(doctorProfileRepository.findByLicenseVerifiedFalse());
    }

    @PostMapping("/verify/{doctorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> verifyDoctor(
            @PathVariable UUID doctorId,
            @AuthenticationPrincipal User adminUser
    ) {
        authService.verifyDoctorLicense(doctorId, adminUser.getId());
        return ResponseEntity.ok("Doctor license successfully verified.");
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<DoctorProfile> getCurrentDoctorProfile(@AuthenticationPrincipal User user) {
        return doctorProfileRepository.findByUser(user)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
