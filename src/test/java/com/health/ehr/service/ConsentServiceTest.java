package com.health.ehr.service;

import com.health.ehr.dto.ConsentDtos.*;
import com.health.ehr.entity.*;
import com.health.ehr.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {

    @Mock
    private ConsentRequestRepository consentRequestRepository;
    @Mock
    private BreakGlassEventRepository breakGlassEventRepository;
    @Mock
    private DoctorProfileRepository doctorProfileRepository;
    @Mock
    private PatientProfileRepository patientProfileRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private ConsentService consentService;

    private DoctorProfile verifiedDoctor;
    private DoctorProfile unverifiedDoctor;
    private PatientProfile samplePatient;

    @BeforeEach
    void setUp() {
        User docUser = User.builder().id(UUID.randomUUID()).email("doc@hospital.org").role(Role.DOCTOR).build();
        User patientUser = User.builder().id(UUID.randomUUID()).email("patient@domain.com").role(Role.PATIENT).build();

        verifiedDoctor = DoctorProfile.builder()
                .id(UUID.randomUUID())
                .user(docUser)
                .fullName("Dr. Sarah Jenkins")
                .licenseVerified(true)
                .build();

        unverifiedDoctor = DoctorProfile.builder()
                .id(UUID.randomUUID())
                .user(docUser)
                .fullName("Dr. Marcus Vance")
                .licenseVerified(false)
                .build();

        samplePatient = PatientProfile.builder()
                .id(UUID.randomUUID())
                .user(patientUser)
                .fullName("Johnathon Doe")
                .nationalHealthId("NHID-9821-4471")
                .emergencyContactPhone("+15550100005")
                .build();
    }

    @Test
    @DisplayName("Should generate OTP and persist ConsentRequest when doctor is verified")
    void testRequestConsentSuccess() {
        RequestConsentDto dto = new RequestConsentDto(samplePatient.getId(), "Cardiac Evaluation", "FULL_HISTORY");

        when(doctorProfileRepository.findById(verifiedDoctor.getId())).thenReturn(Optional.of(verifiedDoctor));
        when(patientProfileRepository.findById(samplePatient.getId())).thenReturn(Optional.of(samplePatient));
        when(passwordEncoder.encode(anyString())).thenReturn("hashedOtp");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ConsentRequest savedConsent = ConsentRequest.builder()
                .id(UUID.randomUUID())
                .doctor(verifiedDoctor)
                .patient(samplePatient)
                .purpose("Cardiac Evaluation")
                .accessLevel("FULL_HISTORY")
                .status("PENDING")
                .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .build();
        when(consentRequestRepository.save(any(ConsentRequest.class))).thenReturn(savedConsent);

        ConsentResponseDto response = consentService.requestConsent(verifiedDoctor.getId(), dto);

        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
        assertEquals("Dr. Sarah Jenkins", response.getDoctorName());
        assertEquals("Johnathon Doe", response.getPatientName());
        verify(consentRequestRepository).save(any(ConsentRequest.class));
        verify(auditService).logAction(any(), eq("DOCTOR"), eq("CONSENT_REQUESTED"), any(), anyString());
    }

    @Test
    @DisplayName("Should reject consent request if doctor license is not verified")
    void testRequestConsentUnverifiedDoctor() {
        RequestConsentDto dto = new RequestConsentDto(samplePatient.getId(), "Checkup", "FULL_HISTORY");
        when(doctorProfileRepository.findById(unverifiedDoctor.getId())).thenReturn(Optional.of(unverifiedDoctor));
        when(patientProfileRepository.findById(samplePatient.getId())).thenReturn(Optional.of(samplePatient));

        assertThrows(IllegalStateException.class, () -> consentService.requestConsent(unverifiedDoctor.getId(), dto));
        verify(consentRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should verify valid OTP and issue signed 2-hour consent token")
    void testVerifyOtpSuccess() {
        UUID consentId = UUID.randomUUID();
        VerifyOtpDto dto = new VerifyOtpDto(consentId, "749210");

        ConsentRequest consent = ConsentRequest.builder()
                .id(consentId)
                .doctor(verifiedDoctor)
                .patient(samplePatient)
                .purpose("Cardiology consultation")
                .status("PENDING")
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .otpHash("hashedOtp")
                .build();

        when(consentRequestRepository.findById(consentId)).thenReturn(Optional.of(consent));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("consent_otp:" + consentId)).thenReturn("749210");
        when(consentRequestRepository.save(any(ConsentRequest.class))).thenReturn(consent);

        ConsentResponseDto response = consentService.verifyOtp(samplePatient.getUser().getId(), dto);

        assertNotNull(response);
        assertEquals("APPROVED", consent.getStatus());
        assertNotNull(consent.getConsentToken());
        assertTrue(consent.getConsentToken().startsWith("CST-"));
        verify(auditService).logAction(any(), eq("PATIENT"), eq("CONSENT_GRANTED"), any(), anyString());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException on incorrect OTP")
    void testVerifyOtpFailure() {
        UUID consentId = UUID.randomUUID();
        VerifyOtpDto dto = new VerifyOtpDto(consentId, "000000");

        ConsentRequest consent = ConsentRequest.builder()
                .id(consentId)
                .doctor(verifiedDoctor)
                .patient(samplePatient)
                .status("PENDING")
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .otpHash("hashedOtp")
                .build();

        when(consentRequestRepository.findById(consentId)).thenReturn(Optional.of(consent));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("consent_otp:" + consentId)).thenReturn("749210");

        assertThrows(IllegalArgumentException.class, () -> consentService.verifyOtp(UUID.randomUUID(), dto));
    }

    @Test
    @DisplayName("Should successfully record emergency Break-Glass override event")
    void testTriggerBreakGlassSuccess() {
        BreakGlassRequestDto dto = new BreakGlassRequestDto(
                samplePatient.getId(),
                "Acute cardiac arrest in resuscitation bay",
                "Unconscious / GCS 6"
        );

        when(doctorProfileRepository.findById(verifiedDoctor.getId())).thenReturn(Optional.of(verifiedDoctor));
        when(patientProfileRepository.findById(samplePatient.getId())).thenReturn(Optional.of(samplePatient));

        BreakGlassEvent event = BreakGlassEvent.builder()
                .id(UUID.randomUUID())
                .doctor(verifiedDoctor)
                .patient(samplePatient)
                .emergencyReason(dto.getEmergencyReason())
                .patientCondition(dto.getPatientCondition())
                .overrideToken("BG-OVERRIDE-12345")
                .triggeredAt(Instant.now())
                .build();
        when(breakGlassEventRepository.save(any(BreakGlassEvent.class))).thenReturn(event);

        BreakGlassResponseDto response = consentService.triggerBreakGlass(verifiedDoctor.getId(), dto);

        assertNotNull(response);
        assertEquals("BG-OVERRIDE-12345", response.getOverrideToken());
        assertTrue(response.getComplianceAlert().contains("CRITICAL"));
        verify(breakGlassEventRepository).save(any(BreakGlassEvent.class));
        verify(auditService).logAction(any(), eq("DOCTOR"), eq("BREAK_GLASS_OVERRIDE"), any(), anyString());
    }
}
