package com.hospital.dao;

import com.hospital.exception.RecordNotFoundException;
import com.hospital.model.Doctor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Doctor records.
 */
public class DoctorDAO {

    public Doctor create(Doctor d) throws SQLException {
        String sql = "INSERT INTO doctors (name, phone, specialization) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.getName());
            ps.setString(2, d.getPhone());
            ps.setString(3, d.getSpecialization());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    d.setId(keys.getInt(1));
                }
            }
            return d;
        }
    }

    public Doctor findById(int id) throws SQLException, RecordNotFoundException {
        String sql = "SELECT * FROM doctors WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        throw new RecordNotFoundException("No doctor found with ID " + id);
    }

    public List<Doctor> findAll() throws SQLException {
        List<Doctor> doctors = new ArrayList<>();
        String sql = "SELECT * FROM doctors ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                doctors.add(mapRow(rs));
            }
        }
        return doctors;
    }

    public boolean update(Doctor d) throws SQLException {
        String sql = "UPDATE doctors SET name = ?, phone = ?, specialization = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, d.getName());
            ps.setString(2, d.getPhone());
            ps.setString(3, d.getSpecialization());
            ps.setInt(4, d.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String unassignSql = "UPDATE patients SET primary_doctor_id = NULL WHERE primary_doctor_id = ?";
        String deleteSql = "DELETE FROM doctors WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Patients assigned to this doctor must not keep pointing at a deleted row.
            try (PreparedStatement unassign = conn.prepareStatement(unassignSql)) {
                unassign.setInt(1, id);
                unassign.executeUpdate();
            }
            try (PreparedStatement delete = conn.prepareStatement(deleteSql)) {
                delete.setInt(1, id);
                return delete.executeUpdate() > 0;
            }
        }
    }

    private Doctor mapRow(ResultSet rs) throws SQLException {
        return new Doctor(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("phone"),
                rs.getString("specialization")
        );
    }
}
