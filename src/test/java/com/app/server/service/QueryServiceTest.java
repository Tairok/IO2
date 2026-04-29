package com.app.server.service;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class QueryServiceTest {

    @Test
    void executeUpdateWritesOkAndCountOnSuccess() throws Exception {
        DataInputStream dis = inputWithUtf("UPDATE users SET role='ADMIN'");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);

        QueryService svc = new QueryService(sql -> 3, (in, o) -> {
            throw new AssertionError("query() should not be called");
        });
        svc.executeUpdate(dis, dos);

        DataInputStream resp = new DataInputStream(new ByteArrayInputStream(out.toByteArray()));
        assertEquals("OK", resp.readUTF());
        assertEquals(3, resp.readInt());
    }

    @Test
    void executeUpdateWritesDbErrorWhenSQLExceptionOccurs() throws Exception {
        DataInputStream dis = inputWithUtf("UPDATE users SET role='ADMIN'");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);

        QueryService svc = new QueryService(sql -> {
            throw new SQLException("boom");
        }, (in, o) -> {
            throw new AssertionError("query() should not be called");
        });
        svc.executeUpdate(dis, dos);

        DataInputStream resp = new DataInputStream(new ByteArrayInputStream(out.toByteArray()));
        assertEquals("ERR\tDB_ERROR", resp.readUTF());
    }

    @Test
    void queryDelegatesToProvidedExecutor() throws Exception {
        AtomicBoolean called = new AtomicBoolean(false);

        QueryService svc = new QueryService(sql -> 0, (in, out) -> {
            String sql = in.readUTF();
            assertEquals("SELECT 1", sql);
            out.writeUTF("OK");
            out.flush();
            called.set(true);
        });

        DataInputStream dis = inputWithUtf("SELECT 1");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);

        svc.query(dis, dos);

        assertTrue(called.get());
        DataInputStream resp = new DataInputStream(new ByteArrayInputStream(out.toByteArray()));
        assertEquals("OK", resp.readUTF());
    }

    private static DataInputStream inputWithUtf(String... values) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buf);
        for (String value : values) {
            out.writeUTF(value);
        }
        out.flush();
        return new DataInputStream(new ByteArrayInputStream(buf.toByteArray()));
    }
}
