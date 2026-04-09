package com.app.server.service;

import com.app.server.Config;
import com.app.server.utils.AppLogger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;

/**
 * Utility class for obtaining JDBC connections and executing queries/updates.
 */
public class DbService {
    /**
     * Returns a new connection to the database from configuration.
     * @throws SQLException if connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                Config.DB_URL,
                Config.DB_USERNAME,
                Config.DB_PASSWORD
        );
    }

    /**
     * Executes a SELECT query with optional parameters.
     * Returns ResultSet; remember to close both ResultSet,
     * PreparedStatement and Connection after use.
     *
     * @param sql    SQL query with ? as placeholders
     * @param params values to substitute for placeholders
     * @return open ResultSet
     * @throws SQLException on SQL error
     */
    public static ResultSet executeQuery(String sql, Object... params) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        setParams(ps, params);
        return ps.executeQuery();
    }

    /**
     * Executes DML (INSERT, UPDATE, DELETE) with optional parameters.
     * Automatically closes Connection and PreparedStatement.
     *
     * @param sql    SQL query with ? as placeholders
     * @param params values to substitute for placeholders
     * @return number of modified rows
     * @throws SQLException on SQL error
     */
    public static int executeUpdate(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, params);
            return ps.executeUpdate();
        }
    }

    /** Helper method for setting parameters in PreparedStatement. */
    private static void setParams(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

        
    /**
     * Initializes the database by executing the init.sql script.
     * This clears existing data and loads initial schema and data.
     */
    public static void initializeDatabase() {
        AppLogger.info("Initializing database...");
        try {
            // Load the init.sql file from resources
            InputStream is = DbService.class.getClassLoader().getResourceAsStream("init.sql");
            if (is == null) {
                AppLogger.error("init.sql file not found in resources");
                return;
            }

            // Read the SQL script
            String sqlScript = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            is.close();

            // Split the script into individual statements
            String[] statements = sqlScript.split(";");

            // Execute each statement
            try (Connection conn = DbService.getConnection();
                 Statement stmt = conn.createStatement()) {

                for (String statement : statements) {
                    statement = statement.trim();
                    if (!statement.isEmpty()) {
                        AppLogger.info("Executing: " + statement.substring(0, Math.min(50, statement.length())) + "...");
                        stmt.execute(statement);
                    }
                }

                AppLogger.info("Database initialization completed successfully.");
            }

        } catch (IOException e) {
            AppLogger.error("Error reading init.sql file", e);
        } catch (SQLException e) {
            AppLogger.error("Error executing database initialization", e);
        }
    }


    /**
     * Returns next ID based on row count: COUNT(*) + 1.
     * Works only if IDs are sequential and no rows are deleted.
     *
     * @param tableName  name of the table
     * @return next ID
     */
    public static int getNextIdSimple(String tableName) {
        String sql = "SELECT COUNT(*) AS cnt FROM " + tableName;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("cnt") + 1;
            }

        } catch (SQLException e) {
            AppLogger.error("Error calculating next simple ID for table " + tableName, e);
        }

        return 1; // fallback
    }
}

