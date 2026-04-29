package com.app.client.service;

import com.app.client.model.FileEntry;
import com.app.client.network.NetworkConnection;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandServiceTest {

    @Test
    void listParsesResponseAndSendsRequest() throws Exception {
        byte[] serverBytes = serverListResponseBytes();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(serverBytes));

        ByteArrayOutputStream clientOut = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(clientOut);

        NetworkConnection conn = connectionWithStreams(dis, dos);
        CommandService svc = new CommandService(conn);

        List<FileEntry> files = svc.list("john");
        assertEquals(2, files.size());
        assertEquals("a.txt", files.get(0).getFilename());
        assertEquals("b.txt", files.get(1).getFilename());

        DataInputStream sent = new DataInputStream(new ByteArrayInputStream(clientOut.toByteArray()));
        assertEquals("LIST", sent.readUTF());
        assertEquals("john", sent.readUTF());
    }

    @Test
    void listThrowsOnErrorStatus() throws Exception {
        ByteArrayOutputStream resp = new ByteArrayOutputStream();
        DataOutputStream serverOut = new DataOutputStream(resp);
        serverOut.writeUTF("ERR\tNO_USER");
        serverOut.flush();

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(resp.toByteArray()));
        DataOutputStream dos = new DataOutputStream(new ByteArrayOutputStream());
        CommandService svc = new CommandService(connectionWithStreams(dis, dos));

        assertThrows(IOException.class, () -> svc.list("missing"));
    }

    @Test
    void executeUpdateReturnsRowCount() throws Exception {
        byte[] server = serverResponse(out -> {
            out.writeUTF("OK");
            out.writeInt(3);
        });

        ByteArrayOutputStream clientOut = new ByteArrayOutputStream();
        CommandService svc = new CommandService(connectionWithStreams(
                new DataInputStream(new ByteArrayInputStream(server)),
                new DataOutputStream(clientOut)
        ));

        int rows = svc.executeUpdate("UPDATE users SET role='ADMIN' WHERE id=1");
        assertEquals(3, rows);

        DataInputStream sent = new DataInputStream(new ByteArrayInputStream(clientOut.toByteArray()));
        assertEquals("EXECUTE", sent.readUTF());
        assertEquals("UPDATE users SET role='ADMIN' WHERE id=1", sent.readUTF());
    }

    @Test
    void getPublicKeyReturnsNullWhenMissing() throws Exception {
        byte[] server = serverResponse(out -> out.writeUTF("ERR\tNOT_FOUND"));
        CommandService svc = new CommandService(connectionWithStreams(
                new DataInputStream(new ByteArrayInputStream(server)),
                new DataOutputStream(new ByteArrayOutputStream())
        ));

        assertNull(svc.getPublicKey("john"));
    }

    @Test
    void storeUserKeysThrowsWhenServerRejects() throws Exception {
        byte[] server = serverResponse(out -> out.writeUTF("ERR\tDB"));
        CommandService svc = new CommandService(connectionWithStreams(
                new DataInputStream(new ByteArrayInputStream(server)),
                new DataOutputStream(new ByteArrayOutputStream())
        ));

        assertThrows(IOException.class, () -> svc.storeUserKeys(
                "john",
                "pub",
                new byte[]{1, 2},
                new byte[]{3},
                new byte[]{4},
                1000
        ));
    }

    private static byte[] serverListResponseBytes() throws IOException {
        ByteArrayOutputStream resp = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(resp);
        out.writeUTF("OK");
        out.writeInt(2);
        out.writeUTF("a.txt\t10\t2024-01-01 10:00:00");
        out.writeUTF("b.txt\t20\t2024-01-01 11:00:00");
        out.flush();
        return resp.toByteArray();
    }

    @FunctionalInterface
    private interface ThrowingWriter {
        void write(DataOutputStream out) throws IOException;
    }

    private static byte[] serverResponse(ThrowingWriter writer) throws IOException {
        ByteArrayOutputStream resp = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(resp);
        writer.write(out);
        out.flush();
        return resp.toByteArray();
    }

    private static NetworkConnection connectionWithStreams(DataInputStream dis, DataOutputStream dos) throws Exception {
        NetworkConnection conn = new NetworkConnection();
        Field disField = NetworkConnection.class.getDeclaredField("dis");
        Field dosField = NetworkConnection.class.getDeclaredField("dos");
        disField.setAccessible(true);
        dosField.setAccessible(true);
        disField.set(conn, dis);
        dosField.set(conn, dos);
        return conn;
    }
}
