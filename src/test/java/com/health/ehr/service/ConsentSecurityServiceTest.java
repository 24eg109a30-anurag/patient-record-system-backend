package com.health.ehr.service;

import com.health.ehr.entity.*;
import com.health.ehr.repository.*;
import com.health.ehr.security.ConsentSecurityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsentSecurityServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private DoctorProfileRepository doctorProfileRepository;
    @Mock
    private PatientProfileRepository patientProfileRepository;
    @Mock
    private ConsentRequestRepository consentRequestRepository;
    @Mock
    private BreakGlassEventRepository breakGlassEventRepository;

    @InjectMocks
    private ConsentSecurityService consentSecurityService;

    private UUID patientProfileId;

    @BeforeEach
    void setUp() {
        patientProfileId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should allow ADMIN access without consent restrictions")
    void testAdminAccessAllowed() {
        User adminUser = User.builder().email("admin@ehr.gov").role(Role.ADMIN).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminUser.getEmail(), null, adminUser.getAuthorities())
        );
        when(userRepository.findByEmail("admin@ehr.gov")).thenReturn(Optional.of(adminUser));

        boolean allowed = consentSecurityService.canAccessPatientRecords(patientProfileId);
        assertTrue(allowed);
    }

    @Test
    @DisplayName("Should allow Patient to view their own records")
    void testPatientSelfAccessAllowed() {
        User patientUser = User.builder().email("patient@ehr.gov").role(Role.PATIENT).build();
        PatientProfile patientProfile = PatientProfile.builder().id(patientProfileId).user(patientUser).build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(patientUser.getEmail(), null, patientUser.getAuthorities())
        );
        when(userRepository.findByEmail("patient@ehr.gov")).thenReturn(Optional.of(patientUser));
        when(patientProfileRepository.findByUser(patientUser)).thenReturn(Optional.of(patientProfile));

        boolean allowed = consentSecurityService.canAccessPatientRecords(patientProfileId);
        assertTrue(allowed);
    }

    @Test
    @DisplayName("Should allow Doctor with active APPROVED consent")
    void testDoctorWithConsentAllowed() {
        User docUser = User.builder().email("doc@ehr.gov").role(Role.DOCTOR).build();
        DoctorProfile docProfile = DoctorProfile.builder()
                .id(UUID.randomUUID())
                .user(docUser)
                .licenseVerified(true)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(docUser.getEmail(), null, docUser.getAuthorities())
        );
        when(userRepository.findByEmail("doc@ehr.gov")).thenReturn(Optional.of(docUser));
        when(doctorProfileRepository.findByUser(docUser)).thenReturn(Optional.of(docProfile));
        when(consentRequestRepository.findActiveConsent(eq(docProfile.getId()), eq(patientProfileId), any(Instant.class)))
                .thenReturn(Optional.of(new ConsentRequest()));

        boolean allowed = consentSecurityService.canAccessPatientRecords(patientProfileId);
        assertTrue(allowed);
    }

    @Test
    @DisplayName("Should allow Doctor with emergency Break-Glass override")
    void testDoctorWithBreakGlassAllowed() {
        User docUser = User.builder().email("doc@ehr.gov").role(Role.DOCTOR).build();
        DoctorProfile docProfile = DoctorProfile.builder()
                .id(UUID.randomUUID())
                .user(docUser)
                .licenseVerified(true)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(docUser.getEmail(), null, docUser.getAuthorities())
        );
        when(userRepository.findByEmail("doc@ehr.gov")).thenReturn(Optional.of(docUser));
        when(doctorProfileRepository.findByUser(docUser)).thenReturn(Optional.of(docProfile));
        when(consentRequestRepository.findActiveConsent(eq(docProfile.getId()), eq(patientProfileId), any(Instant.class)))
                .thenReturn(Optional.empty());
        when(breakGlassEventRepository.findFirstByDoctorIdAndPatientIdAndTriggeredAtAfterOrderByTriggeredAtDesc(
                eq(docProfile.getId()), eq(patientProfileId), any(Instant.class)))
                .thenReturn(Optional.of(new BreakGlassEvent()));

        boolean allowed = consentSecurityService.canAccessPatientRecords(patientProfileId);
        assertTrue(allowed);
    }

    @Test
    @DisplayName("Should deny Doctor without active consent or break-glass override")
    void testDoctorWithoutAuthorizationDenied() {
        User docUser = User.builder().email("doc@ehr.gov").role(Role.DOCTOR).build();
        DoctorProfile docProfile = DoctorProfile.builder()
                .id(UUID.randomUUID())
                .user(docUser)
                .licenseVerified(true)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(docUser.getEmail(), null, docUser.getAuthorities())
        );
        when(userRepository.findByEmail("doc@ehr.gov")).thenReturn(Optional.of(docUser));
        when(doctorProfileRepository.findByUser(docUser)).thenReturn(Optional.of(docProfile));
        when(consentRequestRepository.findActiveConsent(eq(docProfile.getId()), eq(patientProfileId), any(Instant.class)))
                .thenReturn(Optional.empty());
        when(breakGlassEventRepository.findFirstByDoctorIdAndPatientIdAndTriggeredAtAfterOrderByTriggeredAtDesc(
                eq(docProfile.getId()), eq(patientProfileId), any(Instant.class)))
                .thenReturn(Optional.empty());

        boolean allowed = consentSecurityService.canAccessPatientRecords(patientProfileId);
        assertFalse(allowed);
    }
}
