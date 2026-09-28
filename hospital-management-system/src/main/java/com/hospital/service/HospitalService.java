package com.hospital.service;

import com.hospital.dao.AppointmentDAO;
import com.hospital.dao.DoctorDAO;
import com.hospital.dao.PatientDAO;
import com.hospital.exception.DoubleBookingException;
import com.hospital.exception.RecordNotFoundException;
import com.hospital.model.Appointment;
import com.hospital.model.Doctor;
import com.hospital.model.Patient;
import com.hospital.util.InputValidator;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Business logic layer. The database (via the DAOs) is the source of truth
 * and persistence layer, but this service also keeps HashMap caches keyed
 * by ID for O(1) lookups during a single run, refreshed from the DB.
 */
public class HospitalService {

    private final PatientDAO patientDAO = new PatientDAO();
    private final DoctorDAO doctorDAO = new DoctorDAO();
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();

    // In-memory Map cache: ID -> object, for fast lookups (Collections Framework requirement)
    private final Map<Integer, Patient> patientCache = new HashMap<>();
    private final Map<Integer, Doctor> doctorCache = new HashMap<>();

    public HospitalService() {
        refreshCaches();
    }

    private void refreshCaches() {
        patientCache.clear();
        doctorCache.clear();
        try {
            for (Patient p : patientDAO.findAll()) {
                patientCache.put(p.getId(), p);
            }
            for (Doctor d : doctorDAO.findAll()) {
                doctorCache.put(d.getId(), d);
            }
        } catch (SQLException e) {
            System.err.println("Warning: could not refresh in-memory cache: " + e.getMessage());
        }
    }

    // ---------- Patients ----------

    /** primaryDoctorId may be null; when given, the doctor must exist. */
    public Patient addPatient(String name, String phone, String dob, String address, String diagnosis,
                              Integer primaryDoctorId) throws SQLException, RecordNotFoundException {
        if (primaryDoctorId != null) {
            getDoctor(primaryDoctorId); // throws RecordNotFoundException if the doctor does not exist
        }
        Patient p = new Patient(0, name, phone, dob, address, diagnosis, primaryDoctorId);
        patientDAO.create(p);
        patientCache.put(p.getId(), p);
        return p;
    }

    public List<Patient> listPatients() throws SQLException {
        // ArrayList used for ordered, index-based display (Collections Framework requirement)
        List<Patient> patients = new ArrayList<>(patientDAO.findAll());
        return patients;
    }

    public Patient getPatient(int id) throws SQLException, RecordNotFoundException {
        if (patientCache.containsKey(id)) {
            return patientCache.get(id);
        }
        Patient p = patientDAO.findById(id);
        patientCache.put(id, p);
        return p;
    }

    public List<Patient> searchPatientsByName(String namePart) throws SQLException {
        return patientDAO.searchByName(namePart);
    }

    /** Assigns (or, with null, unassigns) a patient's primary doctor. */
    public void assignDoctor(int patientId, Integer doctorId) throws SQLException, RecordNotFoundException {
        Patient p = getPatient(patientId);
        if (doctorId != null) {
            getDoctor(doctorId);
        }
        p.setPrimaryDoctorId(doctorId);
        updatePatient(p);
    }

    /** Report: every patient assigned to the given doctor. */
    public List<Patient> patientsOfDoctor(int doctorId) throws SQLException, RecordNotFoundException {
        getDoctor(doctorId); // validate the doctor exists
        return patientDAO.findByPrimaryDoctor(doctorId);
    }

    public void updatePatient(Patient p) throws SQLException, RecordNotFoundException {
        if (!patientDAO.update(p)) {
            throw new RecordNotFoundException("No patient found with ID " + p.getId());
        }
        patientCache.put(p.getId(), p);
    }

    public void deletePatient(int id) throws SQLException, RecordNotFoundException {
        if (!patientDAO.delete(id)) {
            throw new RecordNotFoundException("No patient found with ID " + id);
        }
        patientCache.remove(id);
    }

    // ---------- Doctors ----------

    public Doctor addDoctor(String name, String phone, String specialization) throws SQLException {
        Doctor d = new Doctor(0, name, phone, specialization);
        doctorDAO.create(d);
        doctorCache.put(d.getId(), d);
        return d;
    }

    public List<Doctor> listDoctors() throws SQLException {
        return new ArrayList<>(doctorDAO.findAll());
    }

    public Doctor getDoctor(int id) throws SQLException, RecordNotFoundException {
        if (doctorCache.containsKey(id)) {
            return doctorCache.get(id);
        }
        Doctor d = doctorDAO.findById(id);
        doctorCache.put(id, d);
        return d;
    }

    public void updateDoctor(Doctor d) throws SQLException, RecordNotFoundException {
        if (!doctorDAO.update(d)) {
            throw new RecordNotFoundException("No doctor found with ID " + d.getId());
        }
        doctorCache.put(d.getId(), d);
    }

    public void deleteDoctor(int id) throws SQLException, RecordNotFoundException {
        if (!doctorDAO.delete(id)) {
            throw new RecordNotFoundException("No doctor found with ID " + id);
        }
        doctorCache.remove(id);
        // DoctorDAO cleared the assignments in the database; drop stale cached references too.
        for (Patient p : patientCache.values()) {
            if (p.getPrimaryDoctorId() != null && p.getPrimaryDoctorId() == id) {
                p.setPrimaryDoctorId(null);
            }
        }
    }

    // ---------- Appointments ----------

    public Appointment scheduleAppointment(int patientId, int doctorId, LocalDateTime dateTime, String notes)
            throws SQLException, RecordNotFoundException, DoubleBookingException {
        // Validate referenced records exist (throws RecordNotFoundException if not)
        getPatient(patientId);
        getDoctor(doctorId);

        if (appointmentDAO.hasConflict(doctorId, dateTime, -1)) {
            throw new DoubleBookingException(
                    "Doctor #" + doctorId + " already has an appointment near " + dateTime);
        }

        Appointment a = new Appointment(0, patientId, doctorId, dateTime, Appointment.Status.SCHEDULED, notes);
        appointmentDAO.create(a);
        return a;
    }

    public void rescheduleAppointment(int appointmentId, LocalDateTime newDateTime)
            throws SQLException, RecordNotFoundException, DoubleBookingException {
        Appointment existing = appointmentDAO.findById(appointmentId);

        if (appointmentDAO.hasConflict(existing.getDoctorId(), newDateTime, appointmentId)) {
            throw new DoubleBookingException(
                    "Doctor #" + existing.getDoctorId() + " already has an appointment near " + newDateTime);
        }

        if (!appointmentDAO.updateStatusAndTime(appointmentId, newDateTime, Appointment.Status.SCHEDULED)) {
            throw new RecordNotFoundException("No appointment found with ID " + appointmentId);
        }
    }

    public void cancelAppointment(int appointmentId) throws SQLException, RecordNotFoundException {
        if (!appointmentDAO.updateStatus(appointmentId, Appointment.Status.CANCELLED)) {
            throw new RecordNotFoundException("No appointment found with ID " + appointmentId);
        }
    }

    public void completeAppointment(int appointmentId) throws SQLException, RecordNotFoundException {
        if (!appointmentDAO.updateStatus(appointmentId, Appointment.Status.COMPLETED)) {
            throw new RecordNotFoundException("No appointment found with ID " + appointmentId);
        }
    }

    public List<Appointment> allAppointmentsSorted() throws SQLException {
        List<Appointment> list = new ArrayList<>(appointmentDAO.findAll());
        Collections.sort(list); // uses Appointment.compareTo (Comparable) - sorting requirement
        return list;
    }

    /**
     * Human-readable appointment line showing the patient, the doctor for this
     * appointment, and the patient's assigned (primary) doctor, so the links
     * between the three records are visible everywhere appointments are listed.
     */
    public String describeAppointment(Appointment a) {
        Patient p = findPatientOrNull(a.getPatientId());
        Doctor d = findDoctorOrNull(a.getDoctorId());
        String patientLabel = p == null ? "#" + a.getPatientId() + " (deleted)" : p.getName() + " (#" + p.getId() + ")";
        String doctorLabel = doctorLabel(d, a.getDoctorId());
        String assignedLabel = "None";
        if (p != null && p.getPrimaryDoctorId() != null) {
            assignedLabel = doctorLabel(findDoctorOrNull(p.getPrimaryDoctorId()), p.getPrimaryDoctorId());
        }
        return String.format("Appt #%d | Patient: %s | Doctor: %s | Patient's assigned doctor: %s | %s | %s%s",
                a.getId(), patientLabel, doctorLabel, assignedLabel,
                a.getDateTime().format(InputValidator.DATETIME_FMT), a.getStatus(),
                (a.getNotes() == null || a.getNotes().isBlank()) ? "" : " | Notes: " + a.getNotes());
    }

    /** Patient line that shows the assigned doctor by name rather than just an ID. */
    public String describePatient(Patient p) {
        String assigned = "None";
        if (p.getPrimaryDoctorId() != null) {
            assigned = doctorLabel(findDoctorOrNull(p.getPrimaryDoctorId()), p.getPrimaryDoctorId());
        }
        return String.format("Patient #%d | %-20s | Phone: %-12s | DOB: %-10s | Diagnosis: %-15s | Assigned Doctor: %s",
                p.getId(), p.getName(), p.getPhone(), p.getDateOfBirth(),
                p.getDiagnosis() == null || p.getDiagnosis().isBlank() ? "N/A" : p.getDiagnosis(),
                assigned);
    }

    private String doctorLabel(Doctor d, int fallbackId) {
        return d == null ? "#" + fallbackId + " (deleted)" : "Dr. " + d.getName() + " (#" + d.getId() + ")";
    }

    // Cache first, then the database; null if the record no longer exists.
    private Patient findPatientOrNull(int id) {
        try {
            return getPatient(id);
        } catch (SQLException | RecordNotFoundException e) {
            return null;
        }
    }

    private Doctor findDoctorOrNull(int id) {
        try {
            return getDoctor(id);
        } catch (SQLException | RecordNotFoundException e) {
            return null;
        }
    }

    /** Report: a doctor's schedule for a given calendar date, sorted by time. */
    public List<Appointment> doctorDailySchedule(int doctorId, java.time.LocalDate date) throws SQLException {
        return appointmentDAO.findByDoctor(doctorId).stream()
                .filter(a -> a.getDateTime().toLocalDate().equals(date))
                .sorted()
                .collect(Collectors.toList());
    }

    /** Report: a patient's full visit history, sorted by time. */
    public List<Appointment> patientVisitHistory(int patientId) throws SQLException {
        List<Appointment> list = appointmentDAO.findByPatient(patientId);
        Collections.sort(list);
        return list;
    }
}
