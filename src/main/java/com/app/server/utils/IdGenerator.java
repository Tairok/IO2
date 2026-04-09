//to do
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
     * @param tableName table name in database (e.g. "users", "files")
     * @return maximum id + 1, or 1 if table is empty
     * @throws SQLException if an error occurs in the query
     * @throws IllegalArgumentException when tableName is null/blank
     */
    public static int nextId(String tableName) throws SQLException {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("Table name cannot be empty");
        }

        // NOTE: tableName is not a PreparedStatement parameter,
        // so it must come from a trusted source (code) to avoid SQL injection.
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

        // if ResultSet had no row (which shouldn't happen),
        // we treat it as an empty table:
        return 1;
    }
}
