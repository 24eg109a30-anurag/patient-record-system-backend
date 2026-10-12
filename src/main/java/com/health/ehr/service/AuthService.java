package com.health.ehr.service;

import com.health.ehr.dto.AuthDtos.*;
import com.health.ehr.entity.*;
import com.health.ehr.repository.*;
import com.health.ehr.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;

    @Transactional
    public AuthResponse registerDoctor(DoctorRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number already registered: " + request.getPhoneNumber());
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(Role.DOCTOR)
                .status("ACTIVE")
                .build();
        user = userRepository.save(user);

        DoctorProfile profile = DoctorProfile.builder()
                .user(user)
                .fullName(request.getFullName())
                .medicalLicenseNumber(request.getMedicalLicenseNumber())
                .licensingAuthority(request.getLicensingAuthority())
                .hospitalAffiliation(request.getHospitalAffiliation())
                .specialization(request.getSpecialization())
                .licenseVerified(false) // Verified via admin/regulatory check
                .build();
        profile = doctorProfileRepository.save(profile);

        String token = jwtService.generateToken(user, Map.of(
                "role", user.getRole().name(),
                "profileId", profile.getId().toString()
        ));

        auditService.logAction(user.getId(), "DOCTOR", "DOCTOR_REGISTERED", null, "Doctor registered with license: " + request.getMedicalLicenseNumber());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .profileId(profile.getId())
                .fullName(profile.getFullName())
                .licenseVerified(profile.isLicenseVerified())
                .build();
    }

    @Transactional
    public AuthResponse registerPatient(PatientRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number already registered: " + request.getPhoneNumber());
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(Role.PATIENT)
                .status("ACTIVE")
                .build();
        user = userRepository.save(user);

        PatientProfile profile = PatientProfile.builder()
                .user(user)
                .nationalHealthId(request.getNationalHealthId())
                .fullName(request.getFullName())
                .dob(request.getDob())
                .gender(request.getGender())
                .bloodGroup(request.getBloodGroup())
                .emergencyContactPhone(request.getEmergencyContactPhone())
                .allergies(request.getAllergies())
                .chronicConditions(request.getChronicConditions())
                .build();
        profile = patientProfileRepository.save(profile);

        String token = jwtService.generateToken(user, Map.of(
                "role", user.getRole().name(),
                "profileId", profile.getId().toString(),
                "healthId", profile.getNationalHealthId()
        ));

        auditService.logAction(user.getId(), "PATIENT", "PATIENT_REGISTERED", profile.getId(), "Patient registered: " + profile.getNationalHealthId());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .profileId(profile.getId())
                .fullName(profile.getFullName())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        UUID profileId = null;
        String fullName = user.getEmail();
        boolean licenseVerified = false;

        if (user.getRole() == Role.DOCTOR) {
            DoctorProfile doc = doctorProfileRepository.findByUser(user).orElse(null);
            if (doc != null) {
                profileId = doc.getId();
                fullName = doc.getFullName();
                licenseVerified = doc.isLicenseVerified();
            }
        } else if (user.getRole() == Role.PATIENT) {
            PatientProfile pat = patientProfileRepository.findByUser(user).orElse(null);
            if (pat != null) {
                profileId = pat.getId();
                fullName = pat.getFullName();
            }
        }

        String token = jwtService.generateToken(user, Map.of(
                "role", user.getRole().name(),
                "profileId", profileId != null ? profileId.toString() : ""
        ));

        auditService.logAction(user.getId(), user.getRole().name(), "USER_LOGIN", profileId, "User authenticated successfully");

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .profileId(profileId)
                .fullName(fullName)
                .licenseVerified(licenseVerified)
                .build();
    }

    @Transactional
    public void verifyDoctorLicense(UUID doctorId, UUID adminUserId) {
        DoctorProfile doc = doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + doctorId));
        doc.setLicenseVerified(true);
        doc.setVerifiedAt(Instant.now());
        doctorProfileRepository.save(doc);

        auditService.logAction(adminUserId, "ADMIN", "DOCTOR_LICENSE_VERIFIED", null, "Verified license for Dr. " + doc.getFullName());
    }
}
