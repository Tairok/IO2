package com.app.server.utils;

import com.app.server.service.DbService;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Generates the next identifier for any table,
 * based on the maximum value of the `id` column.
 */
public final class IdGenerator {
    private IdGenerator() {}

    /**
     * Returns the next ID for the given table.
     *
     * <p>Note: {@code tableName} is interpolated into SQL and therefore must come from a trusted
     * source (code), not from user input.
     *
     * @param tableName table name in database (e.g. {@code "users"}, {@code "files"})
     * @return {@code MAX(id) + 1}, or {@code 1} if the table is empty
     * @throws SQLException if an error occurs in the query
     * @throws IllegalArgumentException when tableName is null/blank
     */
    public static int nextId(String tableName) throws SQLException {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("Table name cannot be empty");
        }

        String sql = String.format(
                "SELECT COALESCE(MAX(id), 0) AS max_id FROM %s",
                tableName
        );

        AppLogger.info("IdGenerator: getting MAX(id) from table `" + tableName + "`");

        try (ResultSet rs = DbService.executeQuery(sql)) {
            if (rs.next()) {
                int max = rs.getInt("max_id");
                AppLogger.info("IdGenerator: znaleziono max_id=" + max);
                return max + 1;
            }
        }

        return 1;
    }
}
