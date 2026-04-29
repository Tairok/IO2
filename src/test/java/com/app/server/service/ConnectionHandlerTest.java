package com.app.server.network;

import com.app.server.Config;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionHandlerTest {

    @Test
    void pingCreateListUploadDownloadDeleteAndQuit() throws Exception {
        String user = "u_" + System.nanoTime();
        Path userDir = Paths.get(Config.RECEIVED_FILES_PATH, user);
        deleteRecursively(userDir);

        try (ServerSocket ss = new ServerSocket(0)) {
            Thread serverThread = new Thread(() -> {
                try (Socket s = ss.accept()) {
                    new ConnectionHandler(s).run();
                } catch (Exception ignored) {
                }
            }, "test-connection-handler");
            serverThread.start();

            try (Socket client = new Socket("127.0.0.1", ss.getLocalPort())) {
                DataInputStream dis = new DataInputStream(client.getInputStream());
                DataOutputStream dos = new DataOutputStream(client.getOutputStream());

                dos.writeUTF("PING");
                dos.flush();
                assertEquals("PONG", dis.readUTF());

                dos.writeUTF("CREATE");
                dos.writeUTF(user);
                dos.writeUTF("a.txt");
                dos.flush();
                assertEquals("OK", dis.readUTF());
                assertTrue(Files.exists(userDir.resolve("a.txt")));

                dos.writeUTF("LIST");
                dos.writeUTF(user);
                dos.flush();
                assertEquals("OK", dis.readUTF());
                int count = dis.readInt();
                assertTrue(count >= 1);
                for (int i = 0; i < count; i++) {
                    dis.readUTF();
                }

                byte[] payload = "hello".getBytes(StandardCharsets.UTF_8);
                dos.writeUTF("UPLOAD");
                dos.writeUTF(user);
                dos.writeUTF("b.bin");
                dos.writeLong(payload.length);
                dos.write(payload);
                dos.flush();
                assertEquals("OK", dis.readUTF());
                assertArrayEquals(payload, Files.readAllBytes(userDir.resolve("b.bin")));

                dos.writeUTF("DOWNLOAD");
                dos.writeUTF(user);
                dos.writeUTF("b.bin");
                dos.flush();
                long len = dis.readLong();
                assertEquals(payload.length, len);
                byte[] got = dis.readNBytes((int) len);
                assertArrayEquals(payload, got);

                dos.writeUTF("DELETE");
                dos.writeUTF(user);
                dos.writeUTF("b.bin");
                dos.flush();
                assertEquals("OK", dis.readUTF());

                dos.writeUTF("NO_SUCH_COMMAND");
                dos.flush();
                assertEquals("ERROR: Unknown command", dis.readUTF());

                dos.writeUTF("QUIT");
                dos.flush();
            }

            serverThread.join(Duration.ofSeconds(2).toMillis());
            assertFalse(serverThread.isAlive(), "handler thread should stop after QUIT");
        } finally {
            deleteRecursively(userDir);
        }
    }

    private static void deleteRecursively(Path path) throws Exception {
        if (path == null || !Files.exists(path)) return;
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ignored) {
                    }
                });
    }
}
