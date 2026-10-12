package com.health.ehr.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lab_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", nullable = false)
    @JsonIgnore
    private MedicalRecord medicalRecord;

    @Column(nullable = false)
    private String testName;

    @Column(nullable = false)
    private String resultValue;

    private String unit;

    private String referenceRange;

    @Builder.Default
    private String status = "NORMAL"; // NORMAL, HIGH, LOW, CRITICAL

    @CreationTimestamp
    private Instant performedAt;
}
