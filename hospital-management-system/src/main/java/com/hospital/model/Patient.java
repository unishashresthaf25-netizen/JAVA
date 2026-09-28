package com.hospital.model;

/**
 * Represents a patient. Extends the abstract Person class.
 * A patient may optionally be assigned to a primary doctor.
 */
public class Patient extends Person {

    private String dateOfBirth; // stored as ISO string yyyy-MM-dd for simplicity
    private String address;
    private String diagnosis;
    private Integer primaryDoctorId; // null when no doctor is assigned

    public Patient(int id, String name, String phone, String dateOfBirth, String address,
                   String diagnosis, Integer primaryDoctorId) {
        super(id, name, phone);
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.diagnosis = diagnosis;
        this.primaryDoctorId = primaryDoctorId;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public Integer getPrimaryDoctorId() {
        return primaryDoctorId;
    }

    public void setPrimaryDoctorId(Integer primaryDoctorId) {
        this.primaryDoctorId = primaryDoctorId;
    }

    @Override
    public String describe() {
        return String.format("Patient #%d | %-20s | Phone: %-12s | DOB: %-10s | Diagnosis: %-15s | Assigned Doctor: %s",
                getId(), getName(), getPhone(), dateOfBirth,
                diagnosis == null || diagnosis.isBlank() ? "N/A" : diagnosis,
                primaryDoctorId == null ? "None" : "#" + primaryDoctorId);
    }
}
