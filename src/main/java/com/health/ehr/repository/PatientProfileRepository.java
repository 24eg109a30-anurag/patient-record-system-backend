package com.health.ehr.repository;

import com.health.ehr.entity.PatientProfile;
import com.health.ehr.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientProfileRepository extends JpaRepository<PatientProfile, UUID> {
    Optional<PatientProfile> findByUser(User user);
    Optional<PatientProfile> findByUserId(UUID userId);
    Optional<PatientProfile> findByNationalHealthId(String nationalHealthId);

    @Query("SELECT p FROM PatientProfile p WHERE " +
           "LOWER(p.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "p.nationalHealthId LIKE CONCAT('%', :query, '%') OR " +
           "p.user.phoneNumber LIKE CONCAT('%', :query, '%')")
    List<PatientProfile> searchPatients(@Param("query") String query);
}
