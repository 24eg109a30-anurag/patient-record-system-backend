package com.health.ehr.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "doctor_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String medicalLicenseNumber;

    @Column(nullable = false)
    private String licensingAuthority;

    @Column(nullable = false)
    private String hospitalAffiliation;

    @Column(nullable = false)
    private String specialization;

    @Builder.Default
    private boolean licenseVerified = false;

    private Instant verifiedAt;
}
