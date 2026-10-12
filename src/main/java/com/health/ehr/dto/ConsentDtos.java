package com.health.ehr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

public class ConsentDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestConsentDto {
        @NotNull
        private UUID patientId;
        @NotBlank
        private String purpose;
        private String accessLevel = "FULL_HISTORY";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyOtpDto {
        @NotNull
        private UUID consentRequestId;
        @NotBlank
        private String otp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BreakGlassRequestDto {
        @NotNull
        private UUID patientId;
        @NotBlank
        private String emergencyReason;
        @NotBlank
        private String patientCondition;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsentResponseDto {
        private UUID consentRequestId;
        private UUID doctorId;
        private String doctorName;
        private UUID patientId;
        private String patientName;
        private String purpose;
        private String accessLevel;
        private String status;
        private String consentToken;
        private Instant requestedAt;
        private Instant expiresAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BreakGlassResponseDto {
        private UUID eventId;
        private String overrideToken;
        private UUID patientId;
        private String patientName;
        private String emergencyReason;
        private Instant triggeredAt;
        private String complianceAlert;
    }
}
