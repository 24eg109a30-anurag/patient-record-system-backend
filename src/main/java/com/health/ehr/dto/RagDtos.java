package com.health.ehr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

public class RagDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClinicalQueryRequest {
        @NotNull
        private UUID patientId;
        @NotBlank
        private String query;
        private Integer topK = 5;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClinicalQueryResponse {
        private String answer;
        private List<Citation> citations;
        private long processingTimeMs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Citation {
        private String documentName;
        private String encounterDate;
        private String excerpt;
        private Double similarityScore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentIngestionRequest {
        @NotNull
        private UUID patientId;
        private UUID recordId;
        @NotBlank
        private String documentTitle;
        @NotBlank
        private String rawContent;
    }
}
