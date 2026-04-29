package com.app.server.service;

import com.app.server.transfer.QueryHandler;
import com.app.server.utils.AppLogger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Service that executes SQL commands received over the wire.
 *
 * <p>The default implementation delegates to {@link DbService} for updates and
 * {@link com.app.server.transfer.QueryHandler} for select queries.
 */
public class QueryService {

    @FunctionalInterface
    public interface UpdateExecutor {
        int execute(String sql) throws SQLException;
    }

    @FunctionalInterface
    public interface QueryExecutor {
        void handle(DataInputStream dis, DataOutputStream dos) throws IOException;
    }

    private final UpdateExecutor updateExecutor;
    private final QueryExecutor queryExecutor;

    public QueryService() {
        this(sql -> DbService.executeUpdate(sql), QueryHandler::handle);
    }

    QueryService(UpdateExecutor updateExecutor, QueryExecutor queryExecutor) {
        this.updateExecutor = updateExecutor;
        this.queryExecutor = queryExecutor;
    }

    /**
     * Executes an update statement (INSERT/UPDATE/DELETE).
     *
     * <p>Request: {@code [sql:String]}
     * <br>Response: {@code "OK" + affectedRows:int} or {@code "ERR\tDB_ERROR"}
     *
     * @param dis input stream
     * @param dos output stream
     * @throws IOException when protocol I/O fails
     */
    public void executeUpdate(DataInputStream dis, DataOutputStream dos) throws IOException {
        String sql = dis.readUTF();
        AppLogger.info("EXECUTE: " + sql);
        try {
            int count = updateExecutor.execute(sql);
            dos.writeUTF("OK");
            dos.writeInt(count);
        } catch (SQLException e) {
            AppLogger.error("Błąd EXECUTE", e);
            dos.writeUTF("ERR\tDB_ERROR");
        }
        dos.flush();
    }

    /**
     * Executes a select query.
     *
     * <p>Request: {@code [sql:String]}
     * <br>Response: {@code "OK" + metadata + rows} or {@code "ERR\tDB_ERROR"}
     *
     * @param dis input stream
     * @param dos output stream
     * @throws IOException when protocol I/O fails
     */
    public void query(DataInputStream dis, DataOutputStream dos) throws IOException {
        queryExecutor.handle(dis, dos);
    }

}
