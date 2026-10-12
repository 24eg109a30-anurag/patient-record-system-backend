package com.health.ehr.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "break_glass_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BreakGlassEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private DoctorProfile doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientProfile patient;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String emergencyReason;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String patientCondition;

    @Column(nullable = false, unique = true)
    private String overrideToken;

    @CreationTimestamp
    private Instant triggeredAt;

    @Builder.Default
    private boolean complianceNotified = true;

    @Builder.Default
    private boolean reviewedByCompliance = false;

    @Column(columnDefinition = "TEXT")
    private String reviewNotes;
}
