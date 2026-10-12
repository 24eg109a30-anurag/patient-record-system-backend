package com.health.ehr.config;

import com.health.ehr.entity.*;
import com.health.ehr.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded. Skipping initial data load.");
            return;
        }

        log.info("Seeding initial EHR demonstration data...");

        // 1. Admin User
        User admin = User.builder()
                .email("admin@national-ehr.gov")
                .passwordHash(passwordEncoder.encode("Admin@123456"))
                .phoneNumber("+1-555-010-0001")
                .role(Role.ADMIN)
                .status("ACTIVE")
                .build();
        userRepository.save(admin);

        // 2. Verified Doctor
        User doctorUser1 = User.builder()
                .email("dr.jenkins@stjude.org")
                .passwordHash(passwordEncoder.encode("Doctor@123456"))
                .phoneNumber("+1-555-010-0002")
                .role(Role.DOCTOR)
                .status("ACTIVE")
                .build();
        userRepository.save(doctorUser1);

        DoctorProfile docProfile1 = DoctorProfile.builder()
                .user(doctorUser1)
                .fullName("Dr. Sarah Jenkins, MD, FACC")
                .medicalLicenseNumber("MED-LIC-882194")
                .licensingAuthority("State Medical Board of California")
                .hospitalAffiliation("St. Jude Medical Center")
                .specialization("Interventional Cardiology")
                .licenseVerified(true)
                .verifiedAt(Instant.now().minus(30, ChronoUnit.DAYS))
                .build();
        doctorProfileRepository.save(docProfile1);

        // 3. Unverified Doctor (for testing verification flow)
        User doctorUser2 = User.builder()
                .email("dr.marcus@metrohealth.org")
                .passwordHash(passwordEncoder.encode("Doctor@123456"))
                .phoneNumber("+1-555-010-0003")
                .role(Role.DOCTOR)
                .status("PENDING_VERIFICATION")
                .build();
        userRepository.save(doctorUser2);

        DoctorProfile docProfile2 = DoctorProfile.builder()
                .user(doctorUser2)
                .fullName("Dr. Marcus Vance, DO")
                .medicalLicenseNumber("MED-LIC-993412")
                .licensingAuthority("National Osteopathic Board")
                .hospitalAffiliation("Metropolitan General")
                .specialization("Internal Medicine")
                .licenseVerified(false)
                .build();
        doctorProfileRepository.save(docProfile2);

        // 4. Patient: John Doe (Longitudinal Cardiac Record)
        User patientUser1 = User.builder()
                .email("john.doe@email.com")
                .passwordHash(passwordEncoder.encode("Patient@123456"))
                .phoneNumber("+1-555-010-0004")
                .role(Role.PATIENT)
                .status("ACTIVE")
                .build();
        userRepository.save(patientUser1);

        PatientProfile patient1 = PatientProfile.builder()
                .user(patientUser1)
                .nationalHealthId("NHID-9821-4471")
                .fullName("Johnathon Edward Doe")
                .dob(LocalDate.of(1974, 6, 15))
                .gender("Male")
                .bloodGroup("O-Positive")
                .emergencyContactPhone("+1-555-010-0005")
                .allergies("Penicillin (Anaphylaxis), Sulfa drugs")
                .chronicConditions("Hypertension Stage II, Type 2 Diabetes Mellitus, Hyperlipidemia")
                .build();
        patientProfileRepository.save(patient1);

        // Seed encounters for John Doe
        MedicalRecord enc1 = MedicalRecord.builder()
                .patient(patient1)
                .doctor(docProfile1)
                .hospitalName("St. Jude Heart Institute")
                .encounterType("OUTPATIENT")
                .primaryDiagnosis("Essential Hypertension with Exertional Angina")
                .icd10Code("I10, I20.9")
                .clinicalNotes("Patient reports 3-month history of retrosternal pressure on climbing stairs. EKG reveals sinus rhythm with non-specific ST-T wave abnormalities in V5-V6. Advised nuclear stress test.")
                .encounterDate(Instant.now().minus(90, ChronoUnit.DAYS))
                .build();

        enc1.getPrescriptions().add(Prescription.builder()
                .medicalRecord(enc1)
                .medicationName("Atorvastatin")
                .dosage("40mg")
                .frequency("Once daily at bedtime")
                .durationDays(90)
                .instructions("Take with water; report unexplained muscle aches.")
                .build());
        enc1.getPrescriptions().add(Prescription.builder()
                .medicalRecord(enc1)
                .medicationName("Metoprolol Succinate")
                .dosage("50mg")
                .frequency("Once daily morning")
                .durationDays(90)
                .instructions("Monitor resting pulse; do not stop abruptly.")
                .build());

        enc1.getLabResults().add(LabResult.builder()
                .medicalRecord(enc1)
                .testName("Lipid Panel - Total Cholesterol")
                .resultValue("245")
                .unit("mg/dL")
                .referenceRange("< 200")
                .status("HIGH")
                .build());
        enc1.getLabResults().add(LabResult.builder()
                .medicalRecord(enc1)
                .testName("Lipid Panel - LDL")
                .resultValue("162")
                .unit("mg/dL")
                .referenceRange("< 100")
                .status("HIGH")
                .build());

        medicalRecordRepository.save(enc1);

        MedicalRecord enc2 = MedicalRecord.builder()
                .patient(patient1)
                .doctor(docProfile1)
                .hospitalName("Metropolitan General Hospital")
                .encounterType("EMERGENCY")
                .primaryDiagnosis("Acute Coronary Syndrome Rule-Out (Resolved Non-STEMI)")
                .icd10Code("I21.4")
                .clinicalNotes("Admitted via ED with acute diaphoresis and substernal chest discomfort radiating to left jaw. Troponin-I peaked at 0.08 ng/mL. Coronary angiogram demonstrated 70% proximal LAD stenosis treated with drug-eluting stent placement.")
                .encounterDate(Instant.now().minus(14, ChronoUnit.DAYS))
                .build();

        enc2.getPrescriptions().add(Prescription.builder()
                .medicalRecord(enc2)
                .medicationName("Ticagrelor (Brilinta)")
                .dosage("90mg")
                .frequency("Twice daily")
                .durationDays(365)
                .instructions("Mandatory dual-antiplatelet therapy following DES deployment.")
                .build());
        enc2.getPrescriptions().add(Prescription.builder()
                .medicalRecord(enc2)
                .medicationName("Aspirin")
                .dosage("81mg")
                .frequency("Once daily")
                .durationDays(365)
                .instructions("Cardioprotective baby aspirin.")
                .build());

        enc2.getLabResults().add(LabResult.builder()
                .medicalRecord(enc2)
                .testName("High-Sensitivity Troponin I")
                .resultValue("0.08")
                .unit("ng/mL")
                .referenceRange("< 0.04")
                .status("CRITICAL")
                .build());
        enc2.getLabResults().add(LabResult.builder()
                .medicalRecord(enc2)
                .testName("Serum Creatinine")
                .resultValue("1.1")
                .unit("mg/dL")
                .referenceRange("0.7 - 1.3")
                .status("NORMAL")
                .build());

        medicalRecordRepository.save(enc2);

        // 5. Patient: Alice Smith
        User patientUser2 = User.builder()
                .email("alice.smith@email.com")
                .passwordHash(passwordEncoder.encode("Patient@123456"))
                .phoneNumber("+1-555-010-0006")
                .role(Role.PATIENT)
                .status("ACTIVE")
                .build();
        userRepository.save(patientUser2);

        PatientProfile patient2 = PatientProfile.builder()
                .user(patientUser2)
                .nationalHealthId("NHID-5512-8820")
                .fullName("Alice Marie Smith")
                .dob(LocalDate.of(1988, 11, 22))
                .gender("Female")
                .bloodGroup("A-Positive")
                .emergencyContactPhone("+1-555-010-0007")
                .allergies("Latex, Codeine")
                .chronicConditions("Asthma (Moderate Persistent)")
                .build();
        patientProfileRepository.save(patient2);

        log.info("Seeding completed successfully! Demo accounts ready.");
    }
}
