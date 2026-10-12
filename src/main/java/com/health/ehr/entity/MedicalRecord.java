package com.health.ehr.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "medical_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientProfile patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    private DoctorProfile doctor;

    @Column(nullable = false)
    private String hospitalName;

    @Column(nullable = false)
    private String encounterType; // OUTPATIENT, INPATIENT, EMERGENCY, TELEMEDICINE

    @Column(nullable = false)
    private String primaryDiagnosis;

    private String icd10Code;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String clinicalNotes;

    @CreationTimestamp
    private Instant encounterDate;

    @OneToMany(mappedBy = "medicalRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Prescription> prescriptions = new ArrayList<>();

    @OneToMany(mappedBy = "medicalRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LabResult> labResults = new ArrayList<>();

    @OneToMany(mappedBy = "medicalRecord", cascade = CascadeType.ALL)
    @Builder.Default
    private List<MedicalDocument> documents = new ArrayList<>();
}
