package com.health.ehr.service;

import com.health.ehr.dto.EhrDtos.*;
import com.health.ehr.entity.*;
import com.health.ehr.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicalRecordService {

    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final ConsentRequestRepository consentRequestRepository;
    private final BreakGlassEventRepository breakGlassEventRepository;
    private final AuditService auditService;

    public List<PatientSummaryDto> searchPatients(String query, UUID requestingDoctorId) {
        List<PatientProfile> patients = (query == null || query.isBlank())
                ? patientProfileRepository.findAll()
                : patientProfileRepository.searchPatients(query.trim());

        return patients.stream()
                .map(p -> mapToSummary(p, requestingDoctorId))
                .collect(Collectors.toList());
    }

    public PatientSummaryDto getPatientSummary(UUID patientId, UUID requestingDoctorId) {
        PatientProfile patient = patientProfileRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + patientId));
        return mapToSummary(patient, requestingDoctorId);
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordTimelineDto> getPatientTimeline(UUID patientId, UUID requestingDoctorId) {
        PatientProfile patient = patientProfileRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + patientId));

        if (requestingDoctorId != null) {
            boolean hasConsent = consentRequestRepository.findActiveConsent(
                    requestingDoctorId, patientId, Instant.now()
            ).isPresent();

            boolean hasBreakGlass = breakGlassEventRepository
                    .findFirstByDoctorIdAndPatientIdAndTriggeredAtAfterOrderByTriggeredAtDesc(
                            requestingDoctorId, patientId, Instant.now().minus(2, ChronoUnit.HOURS)
                    ).isPresent();

            if (!hasConsent && !hasBreakGlass) {
                throw new AccessDeniedException("Access Denied: Requires valid patient consent OTP or emergency break-glass override.");
            }

            auditService.logAction(
                    requestingDoctorId,
                    "DOCTOR",
                    "VIEW_MEDICAL_TIMELINE",
                    patientId,
                    "Doctor viewed full patient timeline. BreakGlass=" + hasBreakGlass
            );
        }

        List<MedicalRecord> records = medicalRecordRepository.findFullTimelineByPatientId(patientId);
        return records.stream().map(this::mapToTimelineDto).collect(Collectors.toList());
    }

    @Transactional
    public MedicalRecordTimelineDto createEncounter(UUID doctorId, CreateMedicalRecordDto dto) {
        PatientProfile patient = patientProfileRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + dto.getPatientId()));
        DoctorProfile doctor = doctorId != null ? doctorProfileRepository.findById(doctorId).orElse(null) : null;

        MedicalRecord record = MedicalRecord.builder()
                .patient(patient)
                .doctor(doctor)
                .hospitalName(dto.getHospitalName())
                .encounterType(dto.getEncounterType())
                .primaryDiagnosis(dto.getPrimaryDiagnosis())
                .icd10Code(dto.getIcd10Code())
                .clinicalNotes(dto.getClinicalNotes())
                .encounterDate(Instant.now())
                .build();

        if (dto.getPrescriptions() != null) {
            for (CreatePrescriptionDto pDto : dto.getPrescriptions()) {
                Prescription p = Prescription.builder()
                        .medicalRecord(record)
                        .medicationName(pDto.getMedicationName())
                        .dosage(pDto.getDosage())
                        .frequency(pDto.getFrequency())
                        .durationDays(pDto.getDurationDays())
                        .instructions(pDto.getInstructions())
                        .build();
                record.getPrescriptions().add(p);
            }
        }

        if (dto.getLabResults() != null) {
            for (CreateLabResultDto lDto : dto.getLabResults()) {
                LabResult l = LabResult.builder()
                        .medicalRecord(record)
                        .testName(lDto.getTestName())
                        .resultValue(lDto.getResultValue())
                        .unit(lDto.getUnit())
                        .referenceRange(lDto.getReferenceRange())
                        .status(lDto.getStatus() != null ? lDto.getStatus() : "NORMAL")
                        .performedAt(Instant.now())
                        .build();
                record.getLabResults().add(l);
            }
        }

        record = medicalRecordRepository.save(record);

        auditService.logAction(
                doctorId,
                "DOCTOR",
                "CREATE_MEDICAL_RECORD",
                patient.getId(),
                "Encounter created: " + dto.getPrimaryDiagnosis() + " at " + dto.getHospitalName()
        );

        return mapToTimelineDto(record);
    }

    private PatientSummaryDto mapToSummary(PatientProfile p, UUID requestingDoctorId) {
        boolean activeConsent = false;
        boolean breakGlassActive = false;

        if (requestingDoctorId != null) {
            activeConsent = consentRequestRepository.findActiveConsent(
                    requestingDoctorId, p.getId(), Instant.now()
            ).isPresent();

            breakGlassActive = breakGlassEventRepository
                    .findFirstByDoctorIdAndPatientIdAndTriggeredAtAfterOrderByTriggeredAtDesc(
                            requestingDoctorId, p.getId(), Instant.now().minus(2, ChronoUnit.HOURS)
                    ).isPresent();
        }

        return PatientSummaryDto.builder()
                .id(p.getId())
                .nationalHealthId(p.getNationalHealthId())
                .fullName(p.getFullName())
                .dob(p.getDob())
                .gender(p.getGender())
                .bloodGroup(p.getBloodGroup())
                .emergencyContactPhone(p.getEmergencyContactPhone())
                .allergies(p.getAllergies())
                .chronicConditions(p.getChronicConditions())
                .activeConsent(activeConsent)
                .breakGlassActive(breakGlassActive)
                .build();
    }

    private MedicalRecordTimelineDto mapToTimelineDto(MedicalRecord r) {
        return MedicalRecordTimelineDto.builder()
                .id(r.getId())
                .patientId(r.getPatient().getId())
                .patientName(r.getPatient().getFullName())
                .doctorName(r.getDoctor() != null ? r.getDoctor().getFullName() : "Attending Physician")
                .hospitalName(r.getHospitalName())
                .encounterType(r.getEncounterType())
                .primaryDiagnosis(r.getPrimaryDiagnosis())
                .icd10Code(r.getIcd10Code())
                .clinicalNotes(r.getClinicalNotes())
                .encounterDate(r.getEncounterDate())
                .prescriptions(r.getPrescriptions().stream().map(p -> PrescriptionDto.builder()
                        .id(p.getId())
                        .medicationName(p.getMedicationName())
                        .dosage(p.getDosage())
                        .frequency(p.getFrequency())
                        .durationDays(p.getDurationDays())
                        .instructions(p.getInstructions())
                        .build()).collect(Collectors.toList()))
                .labResults(r.getLabResults().stream().map(l -> LabResultDto.builder()
                        .id(l.getId())
                        .testName(l.getTestName())
                        .resultValue(l.getResultValue())
                        .unit(l.getUnit())
                        .referenceRange(l.getReferenceRange())
                        .status(l.getStatus())
                        .performedAt(l.getPerformedAt())
                        .build()).collect(Collectors.toList()))
                .documents(r.getDocuments().stream().map(d -> DocumentDto.builder()
                        .id(d.getId())
                        .fileName(d.getFileName())
                        .fileType(d.getFileType())
                        .fileSizeBytes(d.getFileSizeBytes())
                        .storageUri(d.getStorageUri())
                        .uploadedAt(d.getUploadedAt())
                        .build()).collect(Collectors.toList()))
                .build();
    }
}
