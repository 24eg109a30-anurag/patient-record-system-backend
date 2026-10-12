package com.health.ehr.repository;

import com.health.ehr.entity.DoctorProfile;
import com.health.ehr.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, UUID> {
    Optional<DoctorProfile> findByUser(User user);
    Optional<DoctorProfile> findByUserId(UUID userId);
    Optional<DoctorProfile> findByMedicalLicenseNumber(String medicalLicenseNumber);
    List<DoctorProfile> findByLicenseVerifiedFalse();
}
