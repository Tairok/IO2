package com.app.server.service;

import com.app.server.transfer.QueryHandler;
import com.app.server.utils.AppLogger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;

public class QueryService {
    /** EXECUTE (INSERT/UPDATE/DELETE) */
    public void executeUpdate(DataInputStream dis, DataOutputStream dos) throws IOException {
        String sql = dis.readUTF();
        AppLogger.info("EXECUTE: " + sql);
        try {
            int count = DbService.executeUpdate(sql);
            dos.writeUTF("OK");
            dos.writeInt(count);
        } catch (SQLException e) {
            AppLogger.error("Błąd EXECUTE", e);
            dos.writeUTF("ERR\tDB_ERROR");
        }
        dos.flush();
    }

    /** QUERY (SELECT) */
    public void query(DataInputStream dis, DataOutputStream dos) throws IOException {
        QueryHandler.handle(dis, dos);
    }

}
