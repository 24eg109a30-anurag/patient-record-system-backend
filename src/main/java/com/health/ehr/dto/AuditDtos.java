package com.health.ehr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

public class AuditDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditLogDto {
        private UUID id;
        private UUID actorId;
        private String actorRole;
        private String action;
        private UUID patientId;
        private String patientName;
        private String ipAddress;
        private String userAgent;
        private String details;
        private Instant loggedAt;
    }
}
