package com.health.ehr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class EhrDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientSummaryDto {
        private UUID id;
        private String nationalHealthId;
        private String fullName;
        private LocalDate dob;
        private String gender;
        private String bloodGroup;
        private String emergencyContactPhone;
        private String allergies;
        private String chronicConditions;
        private boolean activeConsent;
        private boolean breakGlassActive;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateMedicalRecordDto {
        @NotNull
        private UUID patientId;
        @NotBlank
        private String hospitalName;
        @NotBlank
        private String encounterType;
        @NotBlank
        private String primaryDiagnosis;
        private String icd10Code;
        @NotBlank
        private String clinicalNotes;
        private List<CreatePrescriptionDto> prescriptions;
        private List<CreateLabResultDto> labResults;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatePrescriptionDto {
        @NotBlank
        private String medicationName;
        @NotBlank
        private String dosage;
        @NotBlank
        private String frequency;
        @NotNull
        private Integer durationDays;
        private String instructions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateLabResultDto {
        @NotBlank
        private String testName;
        @NotBlank
        private String resultValue;
        private String unit;
        private String referenceRange;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicalRecordTimelineDto {
        private UUID id;
        private UUID patientId;
        private String patientName;
        private String doctorName;
        private String hospitalName;
        private String encounterType;
        private String primaryDiagnosis;
        private String icd10Code;
        private String clinicalNotes;
        private Instant encounterDate;
        private List<PrescriptionDto> prescriptions;
        private List<LabResultDto> labResults;
        private List<DocumentDto> documents;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrescriptionDto {
        private UUID id;
        private String medicationName;
        private String dosage;
        private String frequency;
        private Integer durationDays;
        private String instructions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabResultDto {
        private UUID id;
        private String testName;
        private String resultValue;
        private String unit;
        private String referenceRange;
        private String status;
        private Instant performedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentDto {
        private UUID id;
        private String fileName;
        private String fileType;
        private Long fileSizeBytes;
        private String storageUri;
        private Instant uploadedAt;
    }
}
