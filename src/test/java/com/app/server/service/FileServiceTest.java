package com.app.server.service;

import com.app.server.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileServiceTest {

    private String user;
    private Path userDir;

    @BeforeEach
    void setUp() throws IOException {
        user = "testuser_" + System.nanoTime();
        userDir = Paths.get(Config.RECEIVED_FILES_PATH, user);
        Files.createDirectories(userDir);
    }

    @AfterEach
    void tearDown() throws IOException {
        deleteRecursively(userDir);
    }

    @Test
    void listSkipsPartFilesAndInProgressSiblings() throws Exception {
        Files.writeString(userDir.resolve("a.txt"), "ok", StandardCharsets.UTF_8);
        Files.writeString(userDir.resolve("b.txt.part"), "tmp", StandardCharsets.UTF_8);
        Files.writeString(userDir.resolve("c.txt"), "data", StandardCharsets.UTF_8);
        Files.writeString(userDir.resolve("c.txt.part"), "tmp", StandardCharsets.UTF_8);

        DataInputStream dis = inputWithUtf(user);
        ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(outBuf);

        new FileService().list(dis, dos);

        DataInputStream resp = new DataInputStream(new ByteArrayInputStream(outBuf.toByteArray()));
        assertEquals("OK", resp.readUTF());
        int count = resp.readInt();
        List<String> names = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String line = resp.readUTF();
            names.add(line.split("\t", 3)[0]);
        }

        assertEquals(1, names.size());
        assertEquals("a.txt", names.get(0));
    }

    @Test
    void createCreatesEmptyFile() throws Exception {
        DataInputStream dis = inputWithUtf(user, "new.txt");
        ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(outBuf);

        new FileService().create(dis, dos);

        DataInputStream resp = new DataInputStream(new ByteArrayInputStream(outBuf.toByteArray()));
        assertEquals("OK", resp.readUTF());
        assertTrue(Files.exists(userDir.resolve("new.txt")));
    }

    @Test
    void downloadReturnsMinusOneWhenMissing() throws Exception {
        DataInputStream dis = inputWithUtf(user, "missing.txt");
        ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(outBuf);

        new FileService().download(dis, dos);

        DataInputStream resp = new DataInputStream(new ByteArrayInputStream(outBuf.toByteArray()));
        assertEquals(-1L, resp.readLong());
    }

    @Test
    void downloadWritesFileBytes() throws Exception {
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
        Files.write(userDir.resolve("file.txt"), content);

        DataInputStream dis = inputWithUtf(user, "file.txt");
        ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(outBuf);

        new FileService().download(dis, dos);

        DataInputStream resp = new DataInputStream(new ByteArrayInputStream(outBuf.toByteArray()));
        long len = resp.readLong();
        assertEquals(content.length, len);
        byte[] got = resp.readNBytes((int) len);
        assertArrayEquals(content, got);
    }

    private static DataInputStream inputWithUtf(String... values) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buf);
        for (String v : values) {
            out.writeUTF(v);
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
