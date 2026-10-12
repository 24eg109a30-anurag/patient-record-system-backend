package com.health.ehr.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "consent_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private DoctorProfile doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientProfile patient;

    @Column(nullable = false)
    private String purpose;

    @Builder.Default
    @Column(nullable = false)
    private String accessLevel = "FULL_HISTORY"; // FULL_HISTORY, LABS_ONLY, CURRENT_EPISODE

    @Builder.Default
    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, EXPIRED, REVOKED

    private String otpHash;

    @Column(unique = true)
    private String consentToken;

    @CreationTimestamp
    private Instant requestedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant grantedAt;
}
