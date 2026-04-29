package com.app.server.transfer;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileReceiverTest {

    @Test
    void receiveWritesFileToUserDirectory() throws Exception {
        Path base = Files.createTempDirectory("fr-base-");
        String user = "user";
        String name = "x.txt";
        byte[] content = "abc".getBytes(StandardCharsets.UTF_8);

        DataInputStream dis = request(user, name, content);
        FileReceiver.receive(dis, base.toString());

        Path written = base.resolve(user).resolve(name);
        assertTrue(Files.exists(written));
        assertArrayEquals(content, Files.readAllBytes(written));

        Files.walk(base)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ignored) {
                    }
                });
    }

    private static DataInputStream request(String user, String name, byte[] content) throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buf);
        out.writeUTF(user);
        out.writeUTF(name);
        out.writeLong(content.length);
        out.write(content);
        out.flush();
        return new DataInputStream(new ByteArrayInputStream(buf.toByteArray()));
    }
}
