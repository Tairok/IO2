package com.app.server.service;

import com.app.server.Config;
import com.app.server.model.User;
import com.app.server.repository.FileRepository;
import com.app.server.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FileServiceUploadDeleteTest {

    private static class StubUserRepository extends UserRepository {
        final Map<String, User> users = new HashMap<>();

        @Override
        public Optional<User> findByLogin(String login) {
            return Optional.ofNullable(users.get(login));
        }
    }

    private static class StubFileRepository extends FileRepository {
        int saveCalls;
        User savedUser;
        String savedFilename;
        long savedSize;

        boolean deleteMetadataResult;

        @Override
        public void save(User user, String filename, long size) {
            saveCalls++;
            savedUser = user;
            savedFilename = filename;
            savedSize = size;
        }

        @Override
        public boolean deleteMetadata(User user, String filename) {
            return deleteMetadataResult;
        }
    }

    @Test
    void uploadWritesFileAndSendsOkTwice() throws Exception {
        String login = "u_" + System.nanoTime();
        String filename = "upload.txt";
        byte[] content = "hello-upload".getBytes(StandardCharsets.UTF_8);

        Path userDir = Paths.get(Config.RECEIVED_FILES_PATH, login);
        deleteRecursively(userDir);

        StubUserRepository userRepo = new StubUserRepository();
        User u = new User();
        u.setId(1);
        u.setLogin(login);
        userRepo.users.put(login, u);

        StubFileRepository fileRepo = new StubFileRepository();
        FileService svc = new FileService(userRepo, fileRepo);

        DataInputStream dis = uploadInput(login, filename, content);
        ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(outBuf);

        svc.upload(dis, dos);

        DataInputStream resp = new DataInputStream(new ByteArrayInputStream(outBuf.toByteArray()));
        assertEquals("OK", resp.readUTF());
        assertEquals("OK", resp.readUTF());

        Path finalFile = userDir.resolve(filename);
        assertTrue(Files.exists(finalFile));
        assertArrayEquals(content, Files.readAllBytes(finalFile));
        assertFalse(Files.exists(userDir.resolve(filename + ".part")));

        assertEquals(1, fileRepo.saveCalls);
        assertSame(u, fileRepo.savedUser);
        assertEquals(filename, fileRepo.savedFilename);
        assertEquals(content.length, fileRepo.savedSize);

        deleteRecursively(userDir);
    }

    @Test
    void deleteReturnsOkWhenDiskDeleteSucceedsEvenIfMetadataFails() throws Exception {
        String login = "u_" + System.nanoTime();
        String filename = "to-delete.txt";

        Path userDir = Paths.get(Config.RECEIVED_FILES_PATH, login);
        deleteRecursively(userDir);
        Files.createDirectories(userDir);
        Files.writeString(userDir.resolve(filename), "x");

        StubUserRepository userRepo = new StubUserRepository();
        User u = new User();
        u.setId(1);
        u.setLogin(login);
        userRepo.users.put(login, u);

        StubFileRepository fileRepo = new StubFileRepository();
        fileRepo.deleteMetadataResult = false;

        FileService svc = new FileService(userRepo, fileRepo);

        DataInputStream dis = inputWithUtf(login, filename);
        ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(outBuf);

        svc.delete(dis, dos);

        DataInputStream resp = new DataInputStream(new ByteArrayInputStream(outBuf.toByteArray()));
        assertEquals("OK", resp.readUTF());
        assertFalse(Files.exists(userDir.resolve(filename)));

        deleteRecursively(userDir);
    }

    @Test
    void deleteReturnsErrWhenNothingWasDeleted() throws Exception {
        String login = "u_" + System.nanoTime();
        String filename = "missing.txt";

        Path userDir = Paths.get(Config.RECEIVED_FILES_PATH, login);
        deleteRecursively(userDir);
        Files.createDirectories(userDir);

        StubUserRepository userRepo = new StubUserRepository();
        User u = new User();
        u.setId(1);
        u.setLogin(login);
        userRepo.users.put(login, u);

        StubFileRepository fileRepo = new StubFileRepository();
        fileRepo.deleteMetadataResult = false;

        FileService svc = new FileService(userRepo, fileRepo);

        DataInputStream dis = inputWithUtf(login, filename);
        ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(outBuf);

        svc.delete(dis, dos);

        DataInputStream resp = new DataInputStream(new ByteArrayInputStream(outBuf.toByteArray()));
        assertEquals("ERR\tDELETE_FAILED", resp.readUTF());

        deleteRecursively(userDir);
    }

    private static DataInputStream uploadInput(String login, String filename, byte[] content) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buf);
        out.writeUTF(login);
        out.writeUTF(filename);
        out.writeLong(content.length);
        out.write(content);
        out.flush();
        return new DataInputStream(new ByteArrayInputStream(buf.toByteArray()));
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

    private static void deleteRecursively(Path path) throws IOException {
        if (path == null || !Files.exists(path)) return;
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                });
    }
}
