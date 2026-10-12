package com.health.ehr.repository;

import com.health.ehr.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByPatientIdOrderByLoggedAtDesc(UUID patientId);
    List<AuditLog> findByActorIdOrderByLoggedAtDesc(UUID actorId);
    List<AuditLog> findTop100ByOrderByLoggedAtDesc();
    List<AuditLog> findByActionOrderByLoggedAtDesc(String action);
}
