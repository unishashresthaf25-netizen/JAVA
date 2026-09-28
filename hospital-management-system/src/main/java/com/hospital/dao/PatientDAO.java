package com.hospital.dao;

import com.hospital.exception.RecordNotFoundException;
import com.hospital.model.Patient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Patient records. All queries use PreparedStatement
 * (no string-concatenated SQL) and try-with-resources for connection cleanup.
 */
public class PatientDAO {

    public Patient create(Patient p) throws SQLException {
        String sql = "INSERT INTO patients (name, phone, date_of_birth, address, diagnosis, primary_doctor_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getPhone());
            ps.setString(3, p.getDateOfBirth());
            ps.setString(4, p.getAddress());
            ps.setString(5, p.getDiagnosis());
            setNullableInt(ps, 6, p.getPrimaryDoctorId());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    p.setId(keys.getInt(1));
                }
            }
            return p;
        }
    }

    public Patient findById(int id) throws SQLException, RecordNotFoundException {
        String sql = "SELECT * FROM patients WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        throw new RecordNotFoundException("No patient found with ID " + id);
    }

    public List<Patient> findAll() throws SQLException {
        List<Patient> patients = new ArrayList<>();
        String sql = "SELECT * FROM patients ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                patients.add(mapRow(rs));
            }
        }
        return patients;
    }

    public List<Patient> searchByName(String namePart) throws SQLException {
        List<Patient> patients = new ArrayList<>();
        String sql = "SELECT * FROM patients WHERE LOWER(name) LIKE ? ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + namePart.toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    patients.add(mapRow(rs));
                }
            }
        }
        return patients;
    }

    public boolean update(Patient p) throws SQLException {
        String sql = "UPDATE patients SET name = ?, phone = ?, date_of_birth = ?, address = ?, diagnosis = ?, primary_doctor_id = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getPhone());
            ps.setString(3, p.getDateOfBirth());
            ps.setString(4, p.getAddress());
            ps.setString(5, p.getDiagnosis());
            setNullableInt(ps, 6, p.getPrimaryDoctorId());
            ps.setInt(7, p.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM patients WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /** Returns every patient whose assigned (primary) doctor is the given doctor. */
    public List<Patient> findByPrimaryDoctor(int doctorId) throws SQLException {
        List<Patient> patients = new ArrayList<>();
        String sql = "SELECT * FROM patients WHERE primary_doctor_id = ? ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    patients.add(mapRow(rs));
                }
            }
        }
        return patients;
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private Patient mapRow(ResultSet rs) throws SQLException {
        int doctorIdValue = rs.getInt("primary_doctor_id");
        Integer primaryDoctorId = rs.wasNull() ? null : doctorIdValue;
        return new Patient(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("phone"),
                rs.getString("date_of_birth"),
                rs.getString("address"),
                rs.getString("diagnosis"),
                primaryDoctorId
        );
    }
}
