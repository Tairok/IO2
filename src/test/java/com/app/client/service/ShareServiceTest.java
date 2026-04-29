package com.app.client.service;

import com.app.client.network.NetworkConnection;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class ShareServiceTest {

    @Test
    void shareFileSendsRequestAndReturnsTrueOnOk() throws Exception {
        ByteArrayOutputStream serverResp = new ByteArrayOutputStream();
        DataOutputStream serverOut = new DataOutputStream(serverResp);
        serverOut.writeUTF("OK");
        serverOut.flush();

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(serverResp.toByteArray()));
        ByteArrayOutputStream clientOut = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(clientOut);

        ShareService svc = new ShareService(connectionWithStreams(dis, dos));
        byte[] key = new byte[] {1, 2, 3};
        assertTrue(svc.shareFile("alice", "bob", "f.txt", key));

        DataInputStream sent = new DataInputStream(new ByteArrayInputStream(clientOut.toByteArray()));
        assertEquals("SHARE", sent.readUTF());
        assertEquals("alice", sent.readUTF());
        assertEquals("bob", sent.readUTF());
        assertEquals("f.txt", sent.readUTF());
        assertEquals(key.length, sent.readInt());
        assertArrayEquals(key, sent.readNBytes(key.length));
    }

    @Test
    void shareFileThrowsIOExceptionWhenServerRejects() throws Exception {
        ByteArrayOutputStream serverResp = new ByteArrayOutputStream();
        DataOutputStream serverOut = new DataOutputStream(serverResp);
        serverOut.writeUTF("ERR\tUNKNOWN");
        serverOut.flush();

        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(serverResp.toByteArray()));
        DataOutputStream dos = new DataOutputStream(new ByteArrayOutputStream());

        ShareService svc = new ShareService(connectionWithStreams(dis, dos));
        IOException ex = assertThrows(IOException.class, () -> svc.shareFile("a", "b", "c.txt", new byte[0]));
        assertTrue(ex.getMessage().startsWith("Share failed:"));
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
