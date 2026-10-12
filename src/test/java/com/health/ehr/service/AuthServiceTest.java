package com.health.ehr.service;

import com.health.ehr.dto.AuthDtos.*;
import com.health.ehr.entity.*;
import com.health.ehr.repository.*;
import com.health.ehr.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private DoctorProfileRepository doctorProfileRepository;
    @Mock
    private PatientProfileRepository patientProfileRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthService authService;

    private User sampleDoctorUser;
    private DoctorProfile sampleDoctorProfile;

    @BeforeEach
    void setUp() {
        sampleDoctorUser = User.builder()
                .id(UUID.randomUUID())
                .email("dr.jenkins@stjude.org")
                .passwordHash("encodedPass")
                .phoneNumber("+15550100002")
                .role(Role.DOCTOR)
                .status("ACTIVE")
                .build();

        sampleDoctorProfile = DoctorProfile.builder()
                .id(UUID.randomUUID())
                .user(sampleDoctorUser)
                .fullName("Dr. Sarah Jenkins")
                .medicalLicenseNumber("MED-LIC-882194")
                .licensingAuthority("Medical Board")
                .hospitalAffiliation("St. Jude")
                .specialization("Cardiology")
                .licenseVerified(false)
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new doctor")
    void testRegisterDoctorSuccess() {
        DoctorRegisterRequest request = DoctorRegisterRequest.builder()
                .email("dr.jenkins@stjude.org")
                .password("Password@123")
                .phoneNumber("+15550100002")
                .fullName("Dr. Sarah Jenkins")
                .medicalLicenseNumber("MED-LIC-882194")
                .licensingAuthority("Medical Board")
                .hospitalAffiliation("St. Jude")
                .specialization("Cardiology")
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenReturn(sampleDoctorUser);
        when(doctorProfileRepository.save(any(DoctorProfile.class))).thenReturn(sampleDoctorProfile);
        when(jwtService.generateToken(any(User.class), anyMap())).thenReturn("mock.jwt.token");

        AuthResponse response = authService.registerDoctor(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("dr.jenkins@stjude.org", response.getEmail());
        assertEquals(Role.DOCTOR, response.getRole());
        assertFalse(response.isLicenseVerified());
        verify(auditService).logAction(any(), eq("DOCTOR"), eq("DOCTOR_REGISTERED"), isNull(), anyString());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when email already exists")
    void testRegisterDuplicateEmail() {
        DoctorRegisterRequest request = DoctorRegisterRequest.builder()
                .email("existing@health.org")
                .phoneNumber("+15550100002")
                .build();

        when(userRepository.existsByEmail("existing@health.org")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.registerDoctor(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should authenticate valid credentials and issue JWT")
    void testLoginSuccess() {
        LoginRequest loginRequest = new LoginRequest("dr.jenkins@stjude.org", "Password@123", null);

        when(userRepository.findByEmail("dr.jenkins@stjude.org")).thenReturn(Optional.of(sampleDoctorUser));
        when(doctorProfileRepository.findByUser(sampleDoctorUser)).thenReturn(Optional.of(sampleDoctorProfile));
        when(jwtService.generateToken(eq(sampleDoctorUser), anyMap())).thenReturn("valid.jwt.token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("valid.jwt.token", response.getToken());
        assertEquals("dr.jenkins@stjude.org", response.getEmail());
        verify(auditService).logAction(any(), eq("DOCTOR"), eq("USER_LOGIN"), any(), anyString());
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when user not found")
    void testLoginUserNotFound() {
        LoginRequest loginRequest = new LoginRequest("unknown@health.org", "wrongPass", null);
        when(userRepository.findByEmail("unknown@health.org")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("Should verify doctor license by admin")
    void testVerifyDoctorLicense() {
        UUID docId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        when(doctorProfileRepository.findById(docId)).thenReturn(Optional.of(sampleDoctorProfile));
        when(doctorProfileRepository.save(any(DoctorProfile.class))).thenReturn(sampleDoctorProfile);

        authService.verifyDoctorLicense(docId, adminId);

        assertTrue(sampleDoctorProfile.isLicenseVerified());
        assertNotNull(sampleDoctorProfile.getVerifiedAt());
        verify(auditService).logAction(eq(adminId), eq("ADMIN"), eq("DOCTOR_LICENSE_VERIFIED"), isNull(), anyString());
    }
}
