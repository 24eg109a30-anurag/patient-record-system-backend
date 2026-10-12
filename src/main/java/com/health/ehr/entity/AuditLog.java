package com.health.ehr.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID actorId;

    @Column(nullable = false)
    private String actorRole;

    @Column(nullable = false)
    private String action; // LOGIN, PATIENT_SEARCH, RECORD_VIEW, BREAK_GLASS_OVERRIDE, RAG_QUERY, CONSENT_GRANTED

    private UUID patientId;

    private String ipAddress;

    private String userAgent;

    @Column(columnDefinition = "TEXT")
    private String details;

    @CreationTimestamp
    private Instant loggedAt;
}
