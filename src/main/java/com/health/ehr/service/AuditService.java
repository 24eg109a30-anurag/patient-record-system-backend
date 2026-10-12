package com.health.ehr.service;

import com.health.ehr.dto.AuditDtos.AuditLogDto;
import com.health.ehr.entity.AuditLog;
import com.health.ehr.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void logAction(UUID actorId, String actorRole, String action, UUID patientId, String details) {
        String ipAddress = "0.0.0.0";
        String userAgent = "Unknown";

        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                ipAddress = request.getRemoteAddr();
                userAgent = request.getHeader("User-Agent");
            }
        } catch (Exception e) {
            log.warn("Could not capture request context for audit: {}", e.getMessage());
        }

        AuditLog auditLog = AuditLog.builder()
                .actorId(actorId)
                .actorRole(actorRole)
                .action(action)
                .patientId(patientId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .details(details)
                .loggedAt(Instant.now())
                .build();

        auditLogRepository.save(auditLog);
        log.info("AUDIT LOG: [Action: {}] [Actor: {} ({})] [Patient: {}]", action, actorId, actorRole, patientId);
    }

    public List<AuditLogDto> getAllLogs() {
        return auditLogRepository.findTop100ByOrderByLoggedAtDesc().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<AuditLogDto> getLogsByPatient(UUID patientId) {
        return auditLogRepository.findByPatientIdOrderByLoggedAtDesc(patientId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private AuditLogDto mapToDto(AuditLog log) {
        return AuditLogDto.builder()
                .id(log.getId())
                .actorId(log.getActorId())
                .actorRole(log.getActorRole())
                .action(log.getAction())
                .patientId(log.getPatientId())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .details(log.getDetails())
                .loggedAt(log.getLoggedAt())
                .build();
    }
}
