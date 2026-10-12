-- Enable Vector and UUID extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS vector;

-- Drop existing tables in reverse dependency order if resetting
-- (schema will be safely managed or created by Hibernate / SQL scripts)

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone_number VARCHAR(50) UNIQUE NOT NULL,
    role VARCHAR(50) NOT NULL, -- DOCTOR, PATIENT, ADMIN, AUDITOR
    mfa_enabled BOOLEAN DEFAULT FALSE,
    mfa_secret VARCHAR(255),
    status VARCHAR(50) DEFAULT 'ACTIVE', -- ACTIVE, PENDING_VERIFICATION, SUSPENDED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS doctor_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    full_name VARCHAR(255) NOT NULL,
    medical_license_number VARCHAR(100) UNIQUE NOT NULL,
    licensing_authority VARCHAR(255) NOT NULL,
    hospital_affiliation VARCHAR(255) NOT NULL,
    specialization VARCHAR(100) NOT NULL,
    license_verified BOOLEAN DEFAULT FALSE,
    verified_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS patient_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    national_health_id VARCHAR(100) UNIQUE NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    dob DATE NOT NULL,
    gender VARCHAR(20) NOT NULL,
    blood_group VARCHAR(10),
    emergency_contact_phone VARCHAR(50) NOT NULL,
    allergies TEXT,
    chronic_conditions TEXT
);

CREATE TABLE IF NOT EXISTS consent_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id UUID NOT NULL REFERENCES doctor_profiles(id),
    patient_id UUID NOT NULL REFERENCES patient_profiles(id),
    purpose VARCHAR(255) NOT NULL,
    access_level VARCHAR(50) NOT NULL DEFAULT 'FULL_HISTORY', -- FULL_HISTORY, LABS_ONLY, CURRENT_EPISODE
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED, EXPIRED, REVOKED
    otp_hash VARCHAR(255),
    consent_token VARCHAR(255) UNIQUE,
    requested_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    granted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS break_glass_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id UUID NOT NULL REFERENCES doctor_profiles(id),
    patient_id UUID NOT NULL REFERENCES patient_profiles(id),
    emergency_reason TEXT NOT NULL,
    patient_condition TEXT NOT NULL,
    override_token VARCHAR(255) UNIQUE NOT NULL,
    triggered_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    compliance_notified BOOLEAN DEFAULT TRUE,
    reviewed_by_compliance BOOLEAN DEFAULT FALSE,
    review_notes TEXT
);

CREATE TABLE IF NOT EXISTS medical_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patient_profiles(id) ON DELETE CASCADE,
    doctor_id UUID REFERENCES doctor_profiles(id),
    hospital_name VARCHAR(255) NOT NULL,
    encounter_type VARCHAR(50) NOT NULL, -- OUTPATIENT, INPATIENT, EMERGENCY, TELEMEDICINE
    primary_diagnosis VARCHAR(255) NOT NULL,
    icd10_code VARCHAR(50),
    clinical_notes TEXT NOT NULL,
    encounter_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS prescriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    record_id UUID NOT NULL REFERENCES medical_records(id) ON DELETE CASCADE,
    medication_name VARCHAR(255) NOT NULL,
    dosage VARCHAR(100) NOT NULL,
    frequency VARCHAR(100) NOT NULL,
    duration_days INT NOT NULL,
    instructions TEXT
);

CREATE TABLE IF NOT EXISTS lab_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    record_id UUID NOT NULL REFERENCES medical_records(id) ON DELETE CASCADE,
    test_name VARCHAR(255) NOT NULL,
    result_value VARCHAR(100) NOT NULL,
    unit VARCHAR(50),
    reference_range VARCHAR(100),
    status VARCHAR(50) DEFAULT 'NORMAL', -- NORMAL, HIGH, LOW, CRITICAL
    performed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS medical_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patient_profiles(id) ON DELETE CASCADE,
    record_id UUID REFERENCES medical_records(id),
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    storage_uri VARCHAR(500) NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS document_embeddings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID REFERENCES medical_documents(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES patient_profiles(id) ON DELETE CASCADE,
    content_chunk TEXT NOT NULL,
    metadata JSONB,
    embedding vector(1536)
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID,
    actor_role VARCHAR(50) NOT NULL,
    action VARCHAR(100) NOT NULL, -- LOGIN, PATIENT_SEARCH, RECORD_VIEW, BREAK_GLASS_OVERRIDE, RAG_QUERY, CONSENT_GRANTED
    patient_id UUID,
    ip_address VARCHAR(100),
    user_agent VARCHAR(500),
    details JSONB,
    logged_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for high throughput and security lookups
CREATE INDEX IF NOT EXISTS idx_audit_patient ON audit_logs(patient_id);
CREATE INDEX IF NOT EXISTS idx_audit_logged_at ON audit_logs(logged_at DESC);
CREATE INDEX IF NOT EXISTS idx_consent_patient ON consent_requests(patient_id, status);
CREATE INDEX IF NOT EXISTS idx_records_patient ON medical_records(patient_id, encounter_date DESC);
