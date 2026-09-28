package com.hospital.main;

import com.hospital.dao.DatabaseConnection;
import com.hospital.exception.DoubleBookingException;
import com.hospital.exception.RecordNotFoundException;
import com.hospital.model.Appointment;
import com.hospital.model.Doctor;
import com.hospital.model.Patient;
import com.hospital.service.HospitalService;
import com.hospital.util.InputValidator;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;


public class Main {

    private static HospitalService service;
    private static final Scanner scanner = new Scanner(System.in);
    private static final InputValidator input = new InputValidator(scanner);

    public static void main(String[] args) {
        System.out.println("Initializing database...");
        DatabaseConnection.initializeSchema();
        service = new HospitalService(); // created after the schema exists so its caches load correctly
        System.out.println("Ready.\n");

        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = input.readInt("Choose an option: ");
            try {
                switch (choice) {
                    case 1 -> patientMenu();
                    case 2 -> doctorMenu();
                    case 3 -> appointmentMenu();
                    case 4 -> reportsMenu();
                    case 0 -> {
                        running = false;
                        System.out.println("Goodbye!");
                    }
                    default -> System.out.println("Invalid option. Please choose again.");
                }
            } catch (SQLException e) {
                // Meaningful handling instead of a bare stack trace: log and keep the app alive.
                System.out.println("A database error occurred: " + e.getMessage());
            } catch (RecordNotFoundException | DoubleBookingException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        scanner.close();
    }

    private static void printMainMenu() {
        System.out.println(" HOSPITAL MANAGEMENT SYSTEM");
        System.out.println("................................");
        System.out.println("1. Patient Management");
        System.out.println("2. Doctor Management");
        System.out.println("3. Appointment Management");
        System.out.println("4. Reports");
        System.out.println("0. Exit");
    }


    private static void patientMenu() throws SQLException, RecordNotFoundException {
        System.out.println("\n--- Patient Management ---");
        System.out.println("1. Add patient");
        System.out.println("2. View all patients");
        System.out.println("3. Search patient by name");
        System.out.println("4. Update patient");
        System.out.println("5. Delete patient");
        System.out.println("6. Assign / change a patient's doctor");
        System.out.println("7. View a patient's profile with appointments");
        System.out.println("0. Back");
        int choice = input.readInt("Choose an option: ");

        switch (choice) {
            case 1 -> {
                String name = input.readNonEmptyString("Name: ");
                String phone = input.readNonEmptyString("Phone: ");
                LocalDate dob = input.readDate("Date of birth");
                String address = input.readOptionalString("Address (optional): ");
                String diagnosis = input.readOptionalString("Diagnosis (optional): ");
                Integer doctorId = readOptionalDoctorId();
                Patient p = service.addPatient(name, phone, dob.toString(), address, diagnosis, doctorId);
                System.out.println("Added: " + service.describePatient(p));
            }
            case 2 -> {
                List<Patient> patients = service.listPatients();
                if (patients.isEmpty()) {
                    System.out.println("No patients on record.");
                } else {
                    patients.forEach(p -> System.out.println(service.describePatient(p)));
                }
            }
            case 3 -> {
                String term = input.readNonEmptyString("Name contains: ");
                List<Patient> results = service.searchPatientsByName(term);
                if (results.isEmpty()) {
                    System.out.println("No matches found.");
                } else {
                    results.forEach(p -> System.out.println(service.describePatient(p)));
                }
            }
            case 4 -> {
                int id = input.readInt("Patient ID to update: ");
                Patient existing = service.getPatient(id); // throws RecordNotFoundException if missing
                System.out.println("Current: " + service.describePatient(existing));
                existing.setName(input.readNonEmptyString("New name [" + existing.getName() + "]: "));
                existing.setPhone(input.readNonEmptyString("New phone [" + existing.getPhone() + "]: "));
                existing.setDiagnosis(input.readOptionalString("New diagnosis (optional): "));
                service.updatePatient(existing);
                System.out.println("Updated.");
            }
            case 5 -> {
                int id = input.readInt("Patient ID to delete: ");
                service.deletePatient(id);
                System.out.println("Deleted patient #" + id);
            }
            case 6 -> {
                int patientId = input.readInt("Patient ID: ");
                Patient existing = service.getPatient(patientId);
                System.out.println("Current: " + service.describePatient(existing));
                Integer doctorId = readOptionalDoctorId();
                service.assignDoctor(patientId, doctorId);
                System.out.println(doctorId == null
                        ? "Removed the doctor assignment for patient #" + patientId
                        : "Patient #" + patientId + " is now assigned to doctor #" + doctorId);
            }
            case 7 -> {
                int patientId = input.readInt("Patient ID: ");
                Patient patient = service.getPatient(patientId);
                System.out.println(service.describePatient(patient));
                List<Appointment> appointments = service.patientVisitHistory(patientId);
                if (appointments.isEmpty()) {
                    System.out.println("  No appointments for this patient.");
                } else {
                    System.out.println("  Appointments:");
                    appointments.forEach(a -> System.out.println("  " + service.describeAppointment(a)));
                }
            }
            case 0 -> { /* back */ }
            default -> System.out.println("Invalid option.");
        }
    }


    private static void doctorMenu() throws SQLException, RecordNotFoundException {
        System.out.println("\n--- Doctor Management ---");
        System.out.println("1. Add doctor");
        System.out.println("2. View all doctors");
        System.out.println("3. Update doctor");
        System.out.println("4. Delete doctor");
        System.out.println("0. Back");
        int choice = input.readInt("Choose an option: ");

        switch (choice) {
            case 1 -> {
                String name = input.readNonEmptyString("Name: ");
                String phone = input.readNonEmptyString("Phone: ");
                String spec = input.readNonEmptyString("Specialization: ");
                Doctor d = service.addDoctor(name, phone, spec);
                System.out.println("Added: " + d.describe());
            }
            case 2 -> {
                List<Doctor> doctors = service.listDoctors();
                if (doctors.isEmpty()) {
                    System.out.println("No doctors on record.");
                } else {
                    doctors.forEach(d -> System.out.println(d.describe()));
                }
            }
            case 3 -> {
                int id = input.readInt("Doctor ID to update: ");
                Doctor existing = service.getDoctor(id);
                System.out.println("Current: " + existing.describe());
                existing.setName(input.readNonEmptyString("New name [" + existing.getName() + "]: "));
                existing.setPhone(input.readNonEmptyString("New phone [" + existing.getPhone() + "]: "));
                existing.setSpecialization(input.readNonEmptyString("New specialization [" + existing.getSpecialization() + "]: "));
                service.updateDoctor(existing);
                System.out.println("Updated.");
            }
            case 4 -> {
                int id = input.readInt("Doctor ID to delete: ");
                service.deleteDoctor(id);
                System.out.println("Deleted doctor #" + id);
            }
            case 0 -> { /* back */ }
            default -> System.out.println("Invalid option.");
        }
    }

    private static void appointmentMenu() throws SQLException, RecordNotFoundException, DoubleBookingException {
        System.out.println("\n--- Appointment Management ---");
        System.out.println("1. Schedule appointment");
        System.out.println("2. Reschedule appointment");
        System.out.println("3. Cancel appointment");
        System.out.println("4. Mark appointment completed");
        System.out.println("5. View all appointments");
        System.out.println("0. Back");
        int choice = input.readInt("Choose an option: ");

        switch (choice) {
            case 1 -> {
                int patientId = input.readInt("Patient ID: ");
                int doctorId = input.readInt("Doctor ID: ");
                LocalDateTime dt = input.readDateTime("Appointment date/time");
                String notes = input.readOptionalString("Notes (optional): ");
                Appointment a = service.scheduleAppointment(patientId, doctorId, dt, notes);
                System.out.println("Scheduled: " + service.describeAppointment(a));
            }
            case 2 -> {
                int apptId = input.readInt("Appointment ID to reschedule: ");
                LocalDateTime dt = input.readDateTime("New date/time");
                service.rescheduleAppointment(apptId, dt);
                System.out.println("Rescheduled appointment #" + apptId);
            }
            case 3 -> {
                int apptId = input.readInt("Appointment ID to cancel: ");
                service.cancelAppointment(apptId);
                System.out.println("Cancelled appointment #" + apptId);
            }
            case 4 -> {
                int apptId = input.readInt("Appointment ID to mark completed: ");
                service.completeAppointment(apptId);
                System.out.println("Marked appointment #" + apptId + " as completed.");
            }
            case 5 -> {
                List<Appointment> list = service.allAppointmentsSorted();
                if (list.isEmpty()) {
                    System.out.println("No appointments on record.");
                } else {
                    list.forEach(a -> System.out.println(service.describeAppointment(a)));
                }
            }
            case 0 -> { /* back */ }
            default -> System.out.println("Invalid option.");
        }
    }

    // ---------------- Reports ----------------

    private static void reportsMenu() throws SQLException, RecordNotFoundException {
        System.out.println("\n--- Reports ---");
        System.out.println("1. Doctor's daily schedule");
        System.out.println("2. Patient visit history");
        System.out.println("3. Patients assigned to a doctor");
        System.out.println("0. Back");
        int choice = input.readInt("Choose an option: ");

        switch (choice) {
            case 1 -> {
                int doctorId = input.readInt("Doctor ID: ");
                LocalDate date = input.readDate("Date");
                List<Appointment> schedule = service.doctorDailySchedule(doctorId, date);
                if (schedule.isEmpty()) {
                    System.out.println("No appointments for doctor #" + doctorId + " on " + date);
                } else {
                    System.out.println("Schedule for doctor #" + doctorId + " on " + date + ":");
                    schedule.forEach(a -> System.out.println(service.describeAppointment(a)));
                }
            }
            case 2 -> {
                int patientId = input.readInt("Patient ID: ");
                List<Appointment> history = service.patientVisitHistory(patientId);
                if (history.isEmpty()) {
                    System.out.println("No visit history for patient #" + patientId);
                } else {
                    System.out.println("Visit history for patient #" + patientId + ":");
                    history.forEach(a -> System.out.println(service.describeAppointment(a)));
                }
            }
            case 3 -> {
                int doctorId = input.readInt("Doctor ID: ");
                List<Patient> assigned = service.patientsOfDoctor(doctorId);
                if (assigned.isEmpty()) {
                    System.out.println("No patients are assigned to doctor #" + doctorId);
                } else {
                    System.out.println("Patients assigned to doctor #" + doctorId + ":");
                    assigned.forEach(p -> System.out.println(service.describePatient(p)));
                }
            }
            case 0 -> { /* back */ }
            default -> System.out.println("Invalid option.");
        }
    }

    // ---------------- Helpers ----------------

    /** Asks for a doctor ID; entering 0 means "no doctor" and returns null. */
    private static Integer readOptionalDoctorId() {
        int id = input.readInt("Assigned doctor ID (0 for none): ");
        return id == 0 ? null : id;
    }
}
