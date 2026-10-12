package com.health.ehr.repository;

import com.health.ehr.entity.ConsentRequest;
import com.health.ehr.entity.DoctorProfile;
import com.health.ehr.entity.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsentRequestRepository extends JpaRepository<ConsentRequest, UUID> {
    List<ConsentRequest> findByPatientOrderByRequestedAtDesc(PatientProfile patient);
    List<ConsentRequest> findByDoctorOrderByRequestedAtDesc(DoctorProfile doctor);
    Optional<ConsentRequest> findByConsentToken(String consentToken);

    @Query("SELECT c FROM ConsentRequest c WHERE c.doctor.id = :doctorId AND c.patient.id = :patientId " +
           "AND c.status = 'APPROVED' AND c.expiresAt > :now")
    Optional<ConsentRequest> findActiveConsent(
            @Param("doctorId") UUID doctorId,
            @Param("patientId") UUID patientId,
            @Param("now") Instant now
    );
}
