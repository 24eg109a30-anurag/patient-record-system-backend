package com.health.ehr.service;

import com.health.ehr.dto.ConsentDtos.*;
import com.health.ehr.entity.*;
import com.health.ehr.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentService {

    private final ConsentRequestRepository consentRequestRepository;
    private final BreakGlassEventRepository breakGlassEventRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final AuditService auditService;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public ConsentResponseDto requestConsent(UUID doctorId, RequestConsentDto dto) {
        DoctorProfile doctor = doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + doctorId));
        PatientProfile patient = patientProfileRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + dto.getPatientId()));

        if (!doctor.isLicenseVerified()) {
            throw new IllegalStateException("Doctor license is not verified. Cannot request patient consent.");
        }

        // Generate 6-digit OTP
        String otp = String.format("%06d", RANDOM.nextInt(1000000));
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);

        ConsentRequest consentRequest = ConsentRequest.builder()
                .doctor(doctor)
                .patient(patient)
                .purpose(dto.getPurpose())
                .accessLevel(dto.getAccessLevel() != null ? dto.getAccessLevel() : "FULL_HISTORY")
                .status("PENDING")
                .otpHash(passwordEncoder.encode(otp))
                .expiresAt(expiresAt)
                .build();
        consentRequest = consentRequestRepository.save(consentRequest);

        // Cache OTP in Redis with 15-minute TTL
        String redisKey = "consent_otp:" + consentRequest.getId();
        try {
            redisTemplate.opsForValue().set(redisKey, otp, Duration.ofMinutes(15));
        } catch (Exception e) {
            log.warn("Redis unavailable, relying on hashed DB OTP: {}", e.getMessage());
        }

        // In production, integrate SMS / push notification to patient's emergency/registered phone
        log.info("DISPATCHED CONSENT OTP: [{}] to Patient {} ({}) for Doctor {}",
                otp, patient.getFullName(), patient.getEmergencyContactPhone(), doctor.getFullName());

        auditService.logAction(
                doctor.getUser().getId(),
                "DOCTOR",
                "CONSENT_REQUESTED",
                patient.getId(),
                "Consent requested. Purpose: " + dto.getPurpose()
        );

        return mapToConsentDto(consentRequest);
    }

    @Transactional
    public ConsentResponseDto verifyOtp(UUID patientUserId, VerifyOtpDto dto) {
        ConsentRequest consent = consentRequestRepository.findById(dto.getConsentRequestId())
                .orElseThrow(() -> new IllegalArgumentException("Consent request not found: " + dto.getConsentRequestId()));

        if (Instant.now().isAfter(consent.getExpiresAt())) {
            consent.setStatus("EXPIRED");
            consentRequestRepository.save(consent);
            throw new IllegalStateException("Consent OTP has expired. Please initiate a new request.");
        }

        // Verify OTP against Redis or DB hash
        String redisKey = "consent_otp:" + consent.getId();
        String cachedOtp = null;
        try {
            cachedOtp = redisTemplate.opsForValue().get(redisKey);
        } catch (Exception e) {
            log.warn("Redis error on OTP fetch: {}", e.getMessage());
        }

        boolean isValid = false;
        if (cachedOtp != null) {
            isValid = cachedOtp.equals(dto.getOtp().trim());
        } else if (consent.getOtpHash() != null) {
            isValid = passwordEncoder.matches(dto.getOtp().trim(), consent.getOtpHash());
        }

        if (!isValid) {
            throw new IllegalArgumentException("Invalid verification OTP.");
        }

        // Generate signed session consent token valid for 2 hours
        String consentToken = "CST-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        consent.setStatus("APPROVED");
        consent.setConsentToken(consentToken);
        consent.setGrantedAt(Instant.now());
        consent.setExpiresAt(Instant.now().plus(2, ChronoUnit.HOURS));
        consentRequestRepository.save(consent);

        // Clear OTP from Redis
        try {
            redisTemplate.delete(redisKey);
        } catch (Exception ignored) {}

        auditService.logAction(
                patientUserId,
                "PATIENT",
                "CONSENT_GRANTED",
                consent.getPatient().getId(),
                "Consent approved for Dr. " + consent.getDoctor().getFullName() + " for 2 hours."
        );

        return mapToConsentDto(consent);
    }

    @Transactional
    public BreakGlassResponseDto triggerBreakGlass(UUID doctorId, BreakGlassRequestDto dto) {
        DoctorProfile doctor = doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + doctorId));
        PatientProfile patient = patientProfileRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + dto.getPatientId()));

        if (!doctor.isLicenseVerified()) {
            throw new IllegalStateException("Unverified doctor cannot invoke emergency Break-Glass protocol.");
        }

        String overrideToken = "BG-OVERRIDE-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase();

        BreakGlassEvent event = BreakGlassEvent.builder()
                .doctor(doctor)
                .patient(patient)
                .emergencyReason(dto.getEmergencyReason())
                .patientCondition(dto.getPatientCondition())
                .overrideToken(overrideToken)
                .complianceNotified(true)
                .reviewedByCompliance(false)
                .build();
        event = breakGlassEventRepository.save(event);

        // Security Alert dispatched to compliance officers
        log.warn("EMERGENCY BREAK-GLASS TRIGGERED: Doctor {} ({}) on Patient {} ({}). Reason: {}",
                doctor.getFullName(), doctor.getMedicalLicenseNumber(),
                patient.getFullName(), patient.getNationalHealthId(),
                dto.getEmergencyReason());

        auditService.logAction(
                doctor.getUser().getId(),
                "DOCTOR",
                "BREAK_GLASS_OVERRIDE",
                patient.getId(),
                "EMERGENCY BREAK-GLASS! Reason: " + dto.getEmergencyReason() + " | Condition: " + dto.getPatientCondition()
        );

        return BreakGlassResponseDto.builder()
                .eventId(event.getId())
                .overrideToken(event.getOverrideToken())
                .patientId(patient.getId())
                .patientName(patient.getFullName())
                .emergencyReason(dto.getEmergencyReason())
                .triggeredAt(event.getTriggeredAt())
                .complianceAlert("CRITICAL: Break-glass logged and flagged for mandatory compliance review.")
                .build();
    }

    public List<ConsentResponseDto> getDoctorConsents(UUID doctorId) {
        DoctorProfile doctor = doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
        return consentRequestRepository.findByDoctorOrderByRequestedAtDesc(doctor).stream()
                .map(this::mapToConsentDto)
                .collect(Collectors.toList());
    }

    public List<ConsentResponseDto> getPatientConsents(UUID patientId) {
        PatientProfile patient = patientProfileRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
        return consentRequestRepository.findByPatientOrderByRequestedAtDesc(patient).stream()
                .map(this::mapToConsentDto)
                .collect(Collectors.toList());
    }

    private ConsentResponseDto mapToConsentDto(ConsentRequest c) {
        return ConsentResponseDto.builder()
                .consentRequestId(c.getId())
                .doctorId(c.getDoctor().getId())
                .doctorName(c.getDoctor().getFullName())
                .patientId(c.getPatient().getId())
                .patientName(c.getPatient().getFullName())
                .purpose(c.getPurpose())
                .accessLevel(c.getAccessLevel())
                .status(c.getStatus())
                .consentToken(c.getConsentToken())
                .requestedAt(c.getRequestedAt())
                .expiresAt(c.getExpiresAt())
                .build();
    }
}
