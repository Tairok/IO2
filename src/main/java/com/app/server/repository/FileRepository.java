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
        Timestamp uploadedAt = new Timestamp(System.currentTimeMillis());
        try {
            DbService.executeUpdate(
                    INSERT_FILE,
                    user.getId(),
                    filename,
                    size,
                    uploadedAt
            );
            AppLogger.info("File inserted: " + filename + " for user: " + user.getLogin());
        } catch (SQLException e) {
            AppLogger.error("Failed to insert file for user: " + user.getLogin() +
                    ", file: " + filename, e);
        }
    }

    public boolean deleteMetadata(User user, String filename) {
        try {
            int rows = DbService.executeUpdate(
                    DELETE_FILE,
                    user.getId(),
                    filename
            );
            AppLogger.info("Deleted metadata for file=" + filename +
                    " owner=" + user.getLogin() + " rows=" + rows);
            return rows > 0;
        } catch (SQLException e) {
            AppLogger.error("Failed to delete file metadata: " + filename +
                    " for user: " + user.getLogin(), e.fillInStackTrace());
            return false;
        }
    }


}
