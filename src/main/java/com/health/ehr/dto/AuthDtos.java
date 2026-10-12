package com.health.ehr.dto;

import com.health.ehr.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

public class AuthDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoctorRegisterRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;
        @NotBlank
        private String phoneNumber;
        @NotBlank
        private String fullName;
        @NotBlank
        private String medicalLicenseNumber;
        @NotBlank
        private String licensingAuthority;
        @NotBlank
        private String hospitalAffiliation;
        @NotBlank
        private String specialization;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientRegisterRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;
        @NotBlank
        private String phoneNumber;
        @NotBlank
        private String nationalHealthId;
        @NotBlank
        private String fullName;
        @NotNull
        private LocalDate dob;
        @NotBlank
        private String gender;
        private String bloodGroup;
        @NotBlank
        private String emergencyContactPhone;
        private String allergies;
        private String chronicConditions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank
        private String username; // Email or National Health ID
        @NotBlank
        private String password;
        private String mfaCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponse {
        private String token;
        private UUID userId;
        private String email;
        private Role role;
        private UUID profileId;
        private String fullName;
        private boolean mfaRequired;
        private boolean licenseVerified;
    }
}
