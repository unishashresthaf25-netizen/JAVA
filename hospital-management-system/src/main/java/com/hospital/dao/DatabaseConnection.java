package com.hospital.dao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Centralized JDBC connection manager.
 *
 * The database file path is read from config.properties (not hard-coded),
 * so no credentials/paths live in committed source. config.properties is
 * excluded via .gitignore; config.properties.example is committed as a template.
 */
public final class DatabaseConnection {

    private static final String CONFIG_FILE = "config.properties";
    private static String dbUrl;

    private DatabaseConnection() {
    }

    private static synchronized void loadConfig() {
        if (dbUrl != null) {
            return;
        }
        Properties props = new Properties();
        try (InputStream in = DatabaseConnection.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            System.err.println("Warning: could not read config.properties, using defaults. " + e.getMessage());
        }
        String dbFile = props.getProperty("db.file", "hospital.db");
        dbUrl = "jdbc:sqlite:" + dbFile;
    }

    public static Connection getConnection() throws SQLException {
        loadConfig();
        return DriverManager.getConnection(dbUrl);
    }

    /**
     * Creates the schema (tables) on first run if they do not already exist,
     * by executing the bundled schema.sql script.
     */
    public static void initializeSchema() {
        loadConfig();
        String script;
        try {
            Path path = Path.of("schema.sql");
            if (Files.exists(path)) {
                script = Files.readString(path);
            } else {
                // fall back to a resource on the classpath if run from a jar
                InputStream in = DatabaseConnection.class.getClassLoader().getResourceAsStream("schema.sql");
                if (in == null) {
                    throw new IOException("schema.sql not found on disk or classpath");
                }
                script = new String(in.readAllBytes());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load schema.sql: " + e.getMessage(), e);
        }

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            for (String rawStatement : script.split(";")) {
                String sql = rawStatement.trim();
                if (!sql.isEmpty()) {
                    stmt.execute(sql);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize schema: " + e.getMessage(), e);
        }
        migrateExistingDatabase();
    }

    /**
     * Databases created before the assigned-doctor feature lack the
     * patients.primary_doctor_id column. CREATE TABLE IF NOT EXISTS will not
     * add it, so add it here when missing.
     */
    private static void migrateExistingDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            boolean hasColumn = false;
            try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(patients)")) {
                while (rs.next()) {
                    if ("primary_doctor_id".equalsIgnoreCase(rs.getString("name"))) {
                        hasColumn = true;
                    }
                }
            }
            if (!hasColumn) {
                stmt.execute("ALTER TABLE patients ADD COLUMN primary_doctor_id INTEGER REFERENCES doctors(id)");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to migrate database: " + e.getMessage(), e);
        }
    }
}
