package com.app.client.service;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileServiceDelegationTest {

    private static class FakeCmd extends CommandService {
        boolean listCalled = false;
        boolean deleteCalled = false;
        boolean shareCalled = false;
        boolean shareResult = true;
        byte[] lastWrappedKey;

        FakeCmd() { super(new com.app.client.network.NetworkConnection()); }

        @Override
        public java.util.List<com.app.client.model.FileEntry> list(String user) throws IOException {
            listCalled = true;
            return List.of();
        }

        @Override
        public boolean delete(String username, String filename) throws IOException {
            deleteCalled = true;
            return true;
        }

        @Override
        public boolean share(String sender, String recipient, String filename, byte[] wrappedKey) throws IOException {
            shareCalled = true;
            lastWrappedKey = wrappedKey;
            return shareResult;
        }
    }

    private static class FakeTx extends TransferService {
        boolean uploadCalled = false;
        boolean downloadCalled = false;

        FakeTx() throws Exception {
            super(connectionWithStreams());
        }

        @Override
        public boolean upload(String user, File file, java.util.function.BiConsumer<Long, Long> progress) {
            uploadCalled = true;
            return true;
        }

        @Override
        public boolean download(String user, String name, File dest, java.util.function.BiConsumer<Long, Long> progress) {
            downloadCalled = true;
            return true;
        }
    }

    @Test
    void fileServiceDelegatesCorrectly() throws Exception {
        FakeCmd fakeCmd = new FakeCmd();
        FakeTx fakeTx = new FakeTx();
        FileService svc = new FileService(fakeCmd, fakeTx);

        svc.listFiles("u");
        assertTrue(fakeCmd.listCalled);

        svc.deleteFile("u", "f");
        assertTrue(fakeCmd.deleteCalled);

        svc.uploadFile("u", new File("/tmp/x"), (a,b)->{}) ;
        assertTrue(fakeTx.uploadCalled);

        svc.downloadFile("u", "n", new File("/tmp/x"), (a,b)->{});
        assertTrue(fakeTx.downloadCalled);

        svc.shareFile("a","b","c");
        assertTrue(fakeCmd.shareCalled);
        assertNotNull(fakeCmd.lastWrappedKey);
        assertEquals(0, fakeCmd.lastWrappedKey.length);
    }

    @Test
    void shareFileThrowsWhenCommandFails() throws Exception {
        FakeCmd fakeCmd = new FakeCmd();
        fakeCmd.shareResult = false;
        FileService svc = new FileService(fakeCmd, new FakeTx());

        assertThrows(IOException.class, () -> svc.shareFile("sender", "recipient", "file.txt"));
    }

    private static com.app.client.network.NetworkConnection connectionWithStreams() throws Exception {
        com.app.client.network.NetworkConnection conn = new com.app.client.network.NetworkConnection();
        Field disField = com.app.client.network.NetworkConnection.class.getDeclaredField("dis");
        Field dosField = com.app.client.network.NetworkConnection.class.getDeclaredField("dos");
        disField.setAccessible(true);
        dosField.setAccessible(true);
        disField.set(conn, new java.io.DataInputStream(new java.io.ByteArrayInputStream(new byte[0])));
        dosField.set(conn, new java.io.DataOutputStream(new java.io.ByteArrayOutputStream()));
        return conn;
    }
}
