package com.app.client.service;

import com.app.client.network.NetworkConnection;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class TransferServiceClientTest {

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

    @Test
    void uploadSendsBytesAndReceivesAck() throws Exception {
        Path tmp = Files.createTempFile("ts-upload-", ".txt");
        Files.writeString(tmp, "hello-world");
        long total = Files.size(tmp);

        ByteArrayOutputStream serverResp = new ByteArrayOutputStream();
        DataOutputStream serverDos = new DataOutputStream(serverResp);
        serverDos.writeUTF("OK"); // handshake reply
        serverDos.writeUTF("OK"); // final ack
        serverDos.flush();

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(serverResp.toByteArray()));
        ByteArrayOutputStream clientOut = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(clientOut);

        NetworkConnection conn = connectionWithStreams(dis, dos);
        TransferService svc = new TransferService(conn);

        AtomicLong last = new AtomicLong(0);
        boolean ok = svc.upload("user", tmp.toFile(), (s, t) -> last.set(s));
        assertTrue(ok);
        assertEquals(total, last.get());

        // verify that client sent the UPLOAD header and file metadata
        DataInputStream sent = new DataInputStream(new ByteArrayInputStream(clientOut.toByteArray()));
        assertEquals("UPLOAD", sent.readUTF());
        assertEquals("user", sent.readUTF());
        assertEquals(tmp.getFileName().toString(), sent.readUTF());
        assertEquals(total, sent.readLong());

        Files.deleteIfExists(tmp);
    }

    @Test
    void downloadWritesFileLocally() throws Exception {
        byte[] content = "download-data".getBytes();
        ByteArrayOutputStream serverBuf = new ByteArrayOutputStream();
        DataOutputStream serverDos = new DataOutputStream(serverBuf);
        serverDos.writeLong(content.length);
        serverDos.write(content);
        serverDos.flush();

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(serverBuf.toByteArray()));
        ByteArrayOutputStream clientOut = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(clientOut);

        NetworkConnection conn = connectionWithStreams(dis, dos);
        TransferService svc = new TransferService(conn);

        Path dest = Files.createTempFile("ts-down-", ".bin");
        Files.deleteIfExists(dest);

        AtomicLong reported = new AtomicLong(0);
        boolean ok = svc.download("user", "file.bin", dest.toFile(), (s, t) -> reported.set(s));
        assertTrue(ok);
        assertEquals(content.length, reported.get());

        byte[] got = Files.readAllBytes(dest);
        assertArrayEquals(content, got);

        Files.deleteIfExists(dest);
    }

    @Test
    void uploadThrowsQuotaExceededWhenServerRejects() throws Exception {
        Path tmp = Files.createTempFile("ts-upload-", ".txt");
        Files.writeString(tmp, "hello");

        ByteArrayOutputStream serverResp = new ByteArrayOutputStream();
        DataOutputStream serverDos = new DataOutputStream(serverResp);
        serverDos.writeUTF("ERR\tQUOTA_EXCEEDED");
        serverDos.flush();

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(serverResp.toByteArray()));
        DataOutputStream dos = new DataOutputStream(new ByteArrayOutputStream());
        TransferService svc = new TransferService(connectionWithStreams(dis, dos));

        IOException ex = assertThrows(IOException.class, () -> svc.upload("user", tmp.toFile(), (s, t) -> {}));
        assertEquals("Quota exceeded", ex.getMessage());

        Files.deleteIfExists(tmp);
    }

    @Test
    void downloadReturnsFalseWhenServerReportsMissingFile() throws Exception {
        ByteArrayOutputStream serverResp = new ByteArrayOutputStream();
        DataOutputStream serverDos = new DataOutputStream(serverResp);
        serverDos.writeLong(-1L);
        serverDos.flush();

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(serverResp.toByteArray()));
        DataOutputStream dos = new DataOutputStream(new ByteArrayOutputStream());
        TransferService svc = new TransferService(connectionWithStreams(dis, dos));

        Path dest = Files.createTempFile("ts-down-missing-", ".bin");
        Files.deleteIfExists(dest);

        boolean ok = svc.download("user", "missing.bin", dest.toFile(), (s, t) -> {});
        assertFalse(ok);
        assertFalse(Files.exists(dest));
    }
}
