package com.app.server.controller;

import com.app.server.Config;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

class ServerControllerTest {

    @Test
    void handleClientSupportsPingCreateListDownloadAndQuit() throws Exception {
        String user = "u_" + System.nanoTime();
        Path userDir = Paths.get(Config.RECEIVED_FILES_PATH, user);
        deleteRecursively(userDir);

        try (ServerSocket ss = new ServerSocket(0)) {
            Thread serverThread = new Thread(() -> {
                try (Socket s = ss.accept()) {
                    new ServerController().handleClient(s);
                } catch (Exception ignored) {
                }
            }, "test-server-controller");
            serverThread.start();

            try (Socket client = new Socket("127.0.0.1", ss.getLocalPort())) {
                DataInputStream dis = new DataInputStream(client.getInputStream());
                DataOutputStream dos = new DataOutputStream(client.getOutputStream());

                dos.writeUTF("PING");
                dos.flush();
                assertEquals("PONG", dis.readUTF());

                dos.writeUTF("CREATE");
                dos.writeUTF(user);
                dos.writeUTF("created.txt");
                dos.flush();
                assertEquals("OK", dis.readUTF());
                assertTrue(Files.exists(userDir.resolve("created.txt")));

                dos.writeUTF("LIST");
                dos.writeUTF(user);
                dos.flush();
                assertEquals("OK", dis.readUTF());
                int count = dis.readInt();
                assertTrue(count >= 1);
                for (int i = 0; i < count; i++) {
                    dis.readUTF();
                }

                dos.writeUTF("DOWNLOAD");
                dos.writeUTF(user);
                dos.writeUTF("missing.txt");
                dos.flush();
                assertEquals(-1L, dis.readLong());

                dos.writeUTF("SOMETHING_UNKNOWN");
                dos.flush();
                assertEquals("ERR\tUNKNOWN_COMMAND", dis.readUTF());

                dos.writeUTF("QUIT");
                dos.flush();
            }

            serverThread.join(Duration.ofSeconds(2).toMillis());
            assertFalse(serverThread.isAlive(), "controller thread should stop after QUIT");
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
