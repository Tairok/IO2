// com/capp/client/admin/service/SettingsService.java
package com.app.client.service;

import com.app.client.model.Setting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SettingsService {
    private final CommandService svc;

    public SettingsService(CommandService svc) {
        this.svc = svc;
    }

    public List<Setting> list() throws IOException {
        List<String[]> rows = svc.query("SELECT user_id, display_name FROM settings ORDER BY user_id");
        List<Setting> result = new ArrayList<>(rows.size());
        for (String[] row : rows) {
            if (row.length >= 2) {
                int userId = Integer.parseInt(row[0]);
                result.add(new Setting(userId, row[1]));
            }
        }
        return result;
    }

    public void create(Setting setting) throws IOException {
        if (setting == null) {
            throw new IllegalArgumentException("Setting cannot be null");
        }
        String sql = String.format(
                "INSERT INTO settings (user_id, display_name) VALUES (%d, '%s')",
                setting.getUserId(),
                escape(setting.getDisplayName())
        );
        int affected = svc.executeUpdate(sql);
        if (affected == 0) {
            throw new IOException("Setting was not created");
        }
    }

    public void update(Setting setting) throws IOException {
        if (setting == null) {
            throw new IllegalArgumentException("Setting cannot be null");
        }
        String sql = String.format(
                "UPDATE settings SET display_name = '%s' WHERE user_id = %d",
                escape(setting.getDisplayName()),
                setting.getUserId()
        );
        int affected = svc.executeUpdate(sql);
        if (affected == 0) {
            throw new IOException("Setting was not updated");
        }
    }

    public void delete(int userId) throws IOException {
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid userId");
        }
        int affected = svc.executeUpdate("DELETE FROM settings WHERE user_id = " + userId);
        if (affected == 0) {
            throw new IOException("Setting was not deleted");
        }
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("'", "''");
    }
}
