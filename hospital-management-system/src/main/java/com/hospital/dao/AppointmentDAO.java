package com.hospital.dao;

import com.hospital.exception.RecordNotFoundException;
import com.hospital.model.Appointment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Appointment records.
 */
public class AppointmentDAO {

    /** Appointments are treated as 30-minute slots for overlap checking. */
    private static final int SLOT_MINUTES = 30;

    public Appointment create(Appointment a) throws SQLException {
        String sql = "INSERT INTO appointments (patient_id, doctor_id, appointment_time, status, notes) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getPatientId());
            ps.setInt(2, a.getDoctorId());
            ps.setString(3, a.getDateTime().toString());
            ps.setString(4, a.getStatus().name());
            ps.setString(5, a.getNotes());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    a.setId(keys.getInt(1));
                }
            }
            return a;
        }
    }

    public Appointment findById(int id) throws SQLException, RecordNotFoundException {
        String sql = "SELECT * FROM appointments WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        throw new RecordNotFoundException("No appointment found with ID " + id);
    }

    public List<Appointment> findAll() throws SQLException {
        List<Appointment> list = new ArrayList<>();
        String sql = "SELECT * FROM appointments ORDER BY appointment_time";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Appointment> findByDoctor(int doctorId) throws SQLException {
        List<Appointment> list = new ArrayList<>();
        String sql = "SELECT * FROM appointments WHERE doctor_id = ? ORDER BY appointment_time";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public List<Appointment> findByPatient(int patientId) throws SQLException {
        List<Appointment> list = new ArrayList<>();
        String sql = "SELECT * FROM appointments WHERE patient_id = ? ORDER BY appointment_time";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * Returns true if the given doctor already has a SCHEDULED appointment
     * whose slot overlaps the given time. excludeAppointmentId lets a
     * reschedule ignore its own existing row (pass -1 when not needed).
     */
    public boolean hasConflict(int doctorId, LocalDateTime dateTime, int excludeAppointmentId) throws SQLException {
        LocalDateTime windowStart = dateTime.minusMinutes(SLOT_MINUTES - 1);
        LocalDateTime windowEnd = dateTime.plusMinutes(SLOT_MINUTES - 1);

        String sql = "SELECT COUNT(*) FROM appointments " +
                "WHERE doctor_id = ? AND status = 'SCHEDULED' AND id != ? " +
                "AND appointment_time BETWEEN ? AND ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ps.setInt(2, excludeAppointmentId);
            ps.setString(3, windowStart.toString());
            ps.setString(4, windowEnd.toString());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public boolean updateStatusAndTime(int id, LocalDateTime newTime, Appointment.Status status) throws SQLException {
        String sql = "UPDATE appointments SET appointment_time = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newTime.toString());
            ps.setString(2, status.name());
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateStatus(int id, Appointment.Status status) throws SQLException {
        String sql = "UPDATE appointments SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM appointments WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Appointment mapRow(ResultSet rs) throws SQLException {
        return new Appointment(
                rs.getInt("id"),
                rs.getInt("patient_id"),
                rs.getInt("doctor_id"),
                LocalDateTime.parse(rs.getString("appointment_time")),
                Appointment.Status.valueOf(rs.getString("status")),
                rs.getString("notes")
        );
    }
}
