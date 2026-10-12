package com.health.ehr.service;

import com.health.ehr.dto.EhrDtos.*;
import com.health.ehr.entity.*;
import com.health.ehr.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalRecordServiceTest {

    @Mock
    private PatientProfileRepository patientProfileRepository;
    @Mock
    private DoctorProfileRepository doctorProfileRepository;
    @Mock
    private MedicalRecordRepository medicalRecordRepository;
    @Mock
    private ConsentRequestRepository consentRequestRepository;
    @Mock
    private BreakGlassEventRepository breakGlassEventRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private MedicalRecordService medicalRecordService;

    private PatientProfile patient;
    private DoctorProfile doctor;

    @BeforeEach
    void setUp() {
        patient = PatientProfile.builder()
                .id(UUID.randomUUID())
                .fullName("Johnathon Doe")
                .nationalHealthId("NHID-9821-4471")
                .emergencyContactPhone("+15550100005")
                .build();

        doctor = DoctorProfile.builder()
                .id(UUID.randomUUID())
                .fullName("Dr. Sarah Jenkins")
                .licenseVerified(true)
                .build();
    }

    @Test
    @DisplayName("Should return timeline when doctor has active consent")
    void testGetTimelineWithActiveConsent() {
        when(patientProfileRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(consentRequestRepository.findActiveConsent(eq(doctor.getId()), eq(patient.getId()), any(Instant.class)))
                .thenReturn(Optional.of(new ConsentRequest()));

        MedicalRecord record = MedicalRecord.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .doctor(doctor)
                .hospitalName("St. Jude Heart Institute")
                .encounterType("OUTPATIENT")
                .primaryDiagnosis("Essential Hypertension")
                .clinicalNotes("Routine cardiology follow-up.")
                .encounterDate(Instant.now())
                .build();

        when(medicalRecordRepository.findFullTimelineByPatientId(patient.getId())).thenReturn(List.of(record));

        List<MedicalRecordTimelineDto> timeline = medicalRecordService.getPatientTimeline(patient.getId(), doctor.getId());

        assertNotNull(timeline);
        assertEquals(1, timeline.size());
        assertEquals("Essential Hypertension", timeline.get(0).getPrimaryDiagnosis());
        verify(auditService).logAction(eq(doctor.getId()), eq("DOCTOR"), eq("VIEW_MEDICAL_TIMELINE"), eq(patient.getId()), anyString());
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when doctor has neither consent nor break-glass")
    void testGetTimelineAccessDenied() {
        when(patientProfileRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(consentRequestRepository.findActiveConsent(eq(doctor.getId()), eq(patient.getId()), any(Instant.class)))
                .thenReturn(Optional.empty());
        when(breakGlassEventRepository.findFirstByDoctorIdAndPatientIdAndTriggeredAtAfterOrderByTriggeredAtDesc(
                eq(doctor.getId()), eq(patient.getId()), any(Instant.class)))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () ->
                medicalRecordService.getPatientTimeline(patient.getId(), doctor.getId())
        );

        verify(medicalRecordRepository, never()).findFullTimelineByPatientId(any());
    }

    @Test
    @DisplayName("Should successfully create a new clinical encounter")
    void testCreateEncounterSuccess() {
        CreateMedicalRecordDto dto = new CreateMedicalRecordDto(
                patient.getId(),
                "Metropolitan General",
                "OUTPATIENT",
                "Type 2 Diabetes Mellitus",
                "E11.9",
                "HbA1c elevated at 7.4%. Titrated metformin.",
                List.of(new CreatePrescriptionDto("Metformin", "500mg", "Twice daily", 90, "With meals")),
                List.of(new CreateLabResultDto("Hemoglobin A1c", "7.4", "%", "< 5.7", "HIGH"))
        );

        when(patientProfileRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(doctorProfileRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));

        MedicalRecord savedRecord = MedicalRecord.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .doctor(doctor)
                .hospitalName(dto.getHospitalName())
                .encounterType(dto.getEncounterType())
                .primaryDiagnosis(dto.getPrimaryDiagnosis())
                .icd10Code(dto.getIcd10Code())
                .clinicalNotes(dto.getClinicalNotes())
                .encounterDate(Instant.now())
                .build();

        when(medicalRecordRepository.save(any(MedicalRecord.class))).thenReturn(savedRecord);

        MedicalRecordTimelineDto result = medicalRecordService.createEncounter(doctor.getId(), dto);

        assertNotNull(result);
        assertEquals("Type 2 Diabetes Mellitus", result.getPrimaryDiagnosis());
        verify(medicalRecordRepository).save(any(MedicalRecord.class));
        verify(auditService).logAction(eq(doctor.getId()), eq("DOCTOR"), eq("CREATE_MEDICAL_RECORD"), eq(patient.getId()), anyString());
    }
}
