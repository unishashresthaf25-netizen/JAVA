package com.hospital.model;

/**
 * Represents a doctor. Extends the abstract Person class.
 */
public class Doctor extends Person {

    private String specialization;

    public Doctor(int id, String name, String phone, String specialization) {
        super(id, name, phone);
        this.specialization = specialization;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    @Override
    public String describe() {
        return String.format("Doctor #%d  | Dr. %-16s | Phone: %-12s | Specialization: %s",
                getId(), getName(), getPhone(), specialization);
    }
}
