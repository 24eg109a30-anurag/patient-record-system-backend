package com.health.ehr.repository;

import com.health.ehr.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, UUID> {
    List<MedicalRecord> findByPatientIdOrderByEncounterDateDesc(UUID patientId);

    @Query("SELECT r FROM MedicalRecord r LEFT JOIN FETCH r.prescriptions LEFT JOIN FETCH r.labResults WHERE r.patient.id = :patientId ORDER BY r.encounterDate DESC")
    List<MedicalRecord> findFullTimelineByPatientId(@Param("patientId") UUID patientId);
}
