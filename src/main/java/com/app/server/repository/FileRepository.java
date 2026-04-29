package com.app.server.repository;

import com.app.server.model.User;
import com.app.server.service.DbService;
import com.app.server.utils.*;

import java.sql.SQLException;
import java.sql.Timestamp;



public class FileRepository {
    private static final String INSERT_FILE =
            "INSERT INTO files (owner_id, filename, size_bytes, uploaded_at) " +
                    "VALUES (?, ?, ?, ?)";
    private static final String DELETE_FILE =
            "DELETE FROM files WHERE owner_id = ? AND filename = ?";


    public void save(User user, String filename, long size) {
        String base = java.nio.file.Paths.get(filename).getFileName().toString();
        Timestamp uploadedAt = new Timestamp(System.currentTimeMillis());
        try {
            DbService.executeUpdate(
                    INSERT_FILE,
                    user.getId(),
                    base,
                    size,
                    uploadedAt
            );
            AppLogger.info("File inserted: " + base + " for user: " + user.getLogin());
        } catch (SQLException e) {
            AppLogger.error("Failed to insert file for user: " + user.getLogin() +
                    ", file: " + base, e);
        }
    }

    public boolean deleteMetadata(User user, String filename) {
        String base = java.nio.file.Paths.get(filename).getFileName().toString();
        try {
            int rows = DbService.executeUpdate(
                    DELETE_FILE,
                    user.getId(),
                    base
            );
            AppLogger.info("Deleted metadata for file=" + base +
                    " owner=" + user.getLogin() + " rows=" + rows);
            return rows > 0;
        } catch (SQLException e) {
            AppLogger.error("Failed to delete file metadata: " + base +
                    " for user: " + user.getLogin(), e.fillInStackTrace());
            return false;
        }
    }


}
