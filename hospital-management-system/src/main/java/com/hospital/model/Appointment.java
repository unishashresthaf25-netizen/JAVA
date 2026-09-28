package com.hospital.model;

import java.time.LocalDateTime;

/**
 * Represents an appointment linking a patient to a doctor at a specific time.
 */
public class Appointment implements Comparable<Appointment> {

    public enum Status {
        SCHEDULED, COMPLETED, CANCELLED
    }

    private int id;
    private int patientId;
    private int doctorId;
    private LocalDateTime dateTime;
    private Status status;
    private String notes;

    public Appointment(int id, int patientId, int doctorId, LocalDateTime dateTime, Status status, String notes) {
        this.id = id;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.dateTime = dateTime;
        this.status = status;
        this.notes = notes;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public int getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(int doctorId) {
        this.doctorId = doctorId;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Natural ordering by date/time - used with Collections.sort()
     * to satisfy the "sorting via Comparable/Comparator" requirement.
     */
    @Override
    public int compareTo(Appointment other) {
        return this.dateTime.compareTo(other.dateTime);
    }

    @Override
    public String toString() {
        return String.format("Appt #%d | Patient:%d Doctor:%d | %s | %s%s",
                id, patientId, doctorId, dateTime, status,
                (notes == null || notes.isBlank()) ? "" : " | Notes: " + notes);
    }
}
