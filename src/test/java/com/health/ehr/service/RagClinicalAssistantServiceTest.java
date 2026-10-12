package com.health.ehr.service;

import com.health.ehr.dto.RagDtos.*;
import com.health.ehr.entity.*;
import com.health.ehr.repository.MedicalRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagClinicalAssistantServiceTest {

    @Mock
    private MedicalRecordRepository medicalRecordRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private RagClinicalAssistantService ragService;

    private UUID doctorId;
    private UUID patientId;
    private MedicalRecord sampleRecord;

    @BeforeEach
    void setUp() {
        doctorId = UUID.randomUUID();
        patientId = UUID.randomUUID();

        PatientProfile patient = PatientProfile.builder()
                .id(patientId)
                .fullName("Johnathon Doe")
                .build();

        sampleRecord = MedicalRecord.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .hospitalName("St. Jude Heart Institute")
                .encounterType("EMERGENCY")
                .primaryDiagnosis("Acute Coronary Syndrome")
                .clinicalNotes("Troponin elevated at 0.08 ng/mL. Successful stent placed in LAD.")
                .encounterDate(Instant.now())
                .build();

        sampleRecord.getPrescriptions().add(Prescription.builder()
                .medicationName("Ticagrelor")
                .dosage("90mg")
                .frequency("BID")
                .durationDays(365)
                .build());

        sampleRecord.getLabResults().add(LabResult.builder()
                .testName("Troponin I")
                .resultValue("0.08")
                .unit("ng/mL")
                .status("CRITICAL")
                .build());
    }

    @Test
    @DisplayName("Should synthesize clinical inquiry with grounded citations and log action")
    void testQueryPatientRecordsFallback() {
        ClinicalQueryRequest request = new ClinicalQueryRequest(patientId, "Summarize cardiac history", 4);

        when(medicalRecordRepository.findFullTimelineByPatientId(patientId)).thenReturn(List.of(sampleRecord));

        ClinicalQueryResponse response = ragService.queryPatientRecords(doctorId, request);

        assertNotNull(response);
        assertTrue(response.getAnswer().contains("Acute Coronary Syndrome"));
        assertTrue(response.getAnswer().contains("Ticagrelor"));
        assertFalse(response.getCitations().isEmpty());
        assertEquals("Encounter: Acute Coronary Syndrome", response.getCitations().get(0).getDocumentName());
        assertTrue(response.getProcessingTimeMs() >= 0);

        verify(auditService).logAction(eq(doctorId), eq("DOCTOR"), eq("RAG_CLINICAL_QUERY"), eq(patientId), anyString());
    }

    @Test
    @DisplayName("Should return empty record notice when no clinical records exist")
    void testQueryPatientRecordsEmpty() {
        ClinicalQueryRequest request = new ClinicalQueryRequest(patientId, "Check allergies", 4);
        when(medicalRecordRepository.findFullTimelineByPatientId(patientId)).thenReturn(List.of());

        ClinicalQueryResponse response = ragService.queryPatientRecords(doctorId, request);

        assertNotNull(response);
        assertTrue(response.getAnswer().contains("No historical medical records"));
        assertTrue(response.getCitations().isEmpty());
    }
}
