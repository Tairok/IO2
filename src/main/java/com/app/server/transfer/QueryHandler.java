package com.app.server.transfer;

import com.app.server.Config;
import com.app.server.utils.AppLogger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QueryHandler {
    public static void handle(DataInputStream dis, DataOutputStream dos) throws IOException {
        String sql = dis.readUTF();
        AppLogger.info("Wykonuję zapytanie: " + sql);
        try (
                Connection conn = DriverManager.getConnection(
                        Config.DB_URL,
                        Config.DB_USERNAME,
                        Config.DB_PASSWORD);
                Statement stmt = conn.createStatement();
                ResultSet rs     = stmt.executeQuery(sql)
        ) {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            List<String[]> rows = new ArrayList<>();

            while (rs.next()) {
                String[] row = new String[colCount];
                for (int i = 0; i < colCount; i++) {
                    row[i] = rs.getString(i + 1);
                }
                rows.add(row);
            }

            dos.writeUTF("OK");
            dos.writeInt(colCount);
            dos.writeInt(rows.size());
            for (String[] row : rows) {
                for (String val : row) {
                    dos.writeUTF(val != null ? val : "");
                }
            }
        } catch (SQLException e) {
            AppLogger.error("Database error during query execution", e);
            dos.writeUTF("ERR\tDB_ERROR");
        }
        dos.flush();
    }
}
