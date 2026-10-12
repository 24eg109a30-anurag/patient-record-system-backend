package com.health.ehr.repository;

import com.health.ehr.entity.BreakGlassEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BreakGlassEventRepository extends JpaRepository<BreakGlassEvent, UUID> {
    List<BreakGlassEvent> findByPatientIdOrderByTriggeredAtDesc(UUID patientId);
    List<BreakGlassEvent> findByDoctorIdOrderByTriggeredAtDesc(UUID doctorId);
    Optional<BreakGlassEvent> findByOverrideToken(String overrideToken);
    List<BreakGlassEvent> findByReviewedByComplianceFalseOrderByTriggeredAtDesc();

    Optional<BreakGlassEvent> findFirstByDoctorIdAndPatientIdAndTriggeredAtAfterOrderByTriggeredAtDesc(
            UUID doctorId, UUID patientId, Instant after
    );
}
