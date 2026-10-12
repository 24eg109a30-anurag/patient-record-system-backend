package com.health.ehr.security;

import com.health.ehr.entity.*;
import com.health.ehr.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service("consentSecurity")
@RequiredArgsConstructor
public class ConsentSecurityService {

    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final ConsentRequestRepository consentRequestRepository;
    private final BreakGlassEventRepository breakGlassEventRepository;

    /**
     * Checks if current user has permission to access the patient's medical records.
     * Allowed if:
     * 1. User is the patient themselves
     * 2. User is an ADMIN or AUDITOR
     * 3. User is a verified DOCTOR with either:
     *    a. An active APPROVED consent token
     *    b. An emergency BREAK-GLASS override triggered within the last 2 hours
     */
    public boolean canAccessPatientRecords(UUID patientId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        String email = auth.getName();
        User currentUser = userRepository.findByEmail(email).orElse(null);
        if (currentUser == null) {
            return false;
        }

        // Admins and compliance auditors have broad view access
        if (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.AUDITOR) {
            return true;
        }

        // Patient viewing their own records
        if (currentUser.getRole() == Role.PATIENT) {
            Optional<PatientProfile> patientProfile = patientProfileRepository.findByUser(currentUser);
            return patientProfile.isPresent() && patientProfile.get().getId().equals(patientId);
        }

        // Doctor viewing patient records
        if (currentUser.getRole() == Role.DOCTOR) {
            DoctorProfile doctor = doctorProfileRepository.findByUser(currentUser).orElse(null);
            if (doctor == null || !doctor.isLicenseVerified()) {
                return false;
            }

            // Check Active Consent
            boolean hasConsent = consentRequestRepository.findActiveConsent(
                    doctor.getId(),
                    patientId,
                    Instant.now()
            ).isPresent();

            if (hasConsent) {
                return true;
            }

            // Check Emergency Break-Glass within the last 2 hours
            Instant twoHoursAgo = Instant.now().minus(2, ChronoUnit.HOURS);
            return breakGlassEventRepository
                    .findFirstByDoctorIdAndPatientIdAndTriggeredAtAfterOrderByTriggeredAtDesc(
                            doctor.getId(),
                            patientId,
                            twoHoursAgo
                    ).isPresent();
        }

        return false;
    }
}
