package com.app.server.service;

import com.app.server.Config;
import com.app.server.model.User;
import com.app.server.repository.FileRepository;
import com.app.server.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UserFileShareServiceTest {

    private static class StubUserRepository extends UserRepository {
        final Map<String, User> users = new HashMap<>();

        @Override
        public Optional<User> findByLogin(String login) {
            return Optional.ofNullable(users.get(login));
        }
    }

    private static class StubFileRepository extends FileRepository {
        int saveCalls;
        User lastUser;
        String lastFilename;
        long lastSize;

        @Override
        public void save(User user, String filename, long size) {
            saveCalls++;
            lastUser = user;
            lastFilename = filename;
            lastSize = size;
        }
    }

    @Test
    void shareReturnsUnknownSenderWhenSenderIsMissing() throws Exception {
        StubUserRepository userRepo = new StubUserRepository();
        userRepo.users.put("recipient", user("recipient", 2));

        UserFileShareService svc = new UserFileShareService(userRepo, new StubFileRepository());

        DataInputStream dis = inputForShare(
                "missingSender",
                "recipient",
                "file.txt",
                new byte[]{1, 2, 3}
        );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);

        svc.share(dis, dos);

        DataInputStream resp = new DataInputStream(new ByteArrayInputStream(out.toByteArray()));
        assertEquals("ERR\tUNKNOWN_SENDER", resp.readUTF());
    }

    @Test
    void shareCopiesFileAndSavesMetadataForRecipient() throws Exception {
        String sender = "sender_" + System.nanoTime();
        String recipient = "recipient_" + System.nanoTime();
        String filename = "shared.txt";
        byte[] content = "shared-content".getBytes();

        Path senderDir = Paths.get(Config.RECEIVED_FILES_PATH, sender);
        Path recipientDir = Paths.get(Config.RECEIVED_FILES_PATH, recipient);
        deleteRecursively(senderDir);
        deleteRecursively(recipientDir);
        Files.createDirectories(senderDir);
        Files.write(senderDir.resolve(filename), content);

        StubUserRepository userRepo = new StubUserRepository();
        userRepo.users.put(sender, user(sender, 1));
        User recipUser = user(recipient, 2);
        userRepo.users.put(recipient, recipUser);

        StubFileRepository fileRepo = new StubFileRepository();
        UserFileShareService svc = new UserFileShareService(userRepo, fileRepo);

        DataInputStream dis = inputForShare(sender, recipient, filename, new byte[]{9, 8});
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);

        svc.share(dis, dos);

        DataInputStream resp = new DataInputStream(new ByteArrayInputStream(out.toByteArray()));
        assertEquals("OK", resp.readUTF());

        Path dest = recipientDir.resolve(filename);
        assertTrue(Files.exists(dest));
        assertArrayEquals(content, Files.readAllBytes(dest));

        assertEquals(1, fileRepo.saveCalls);
        assertSame(recipUser, fileRepo.lastUser);
        assertTrue(fileRepo.lastFilename.endsWith(filename));
        assertEquals(content.length, fileRepo.lastSize);

        // sender's file should remain
        assertTrue(Files.exists(senderDir.resolve(filename)));

        deleteRecursively(senderDir);
        deleteRecursively(recipientDir);
    }

    @Test
    void shareReturnsFileNotFoundWhenSourceMissing() throws Exception {
        String sender = "sender_" + System.nanoTime();
        String recipient = "recipient_" + System.nanoTime();

        StubUserRepository userRepo = new StubUserRepository();
        userRepo.users.put(sender, user(sender, 1));
        userRepo.users.put(recipient, user(recipient, 2));

        UserFileShareService svc = new UserFileShareService(userRepo, new StubFileRepository());

        DataInputStream dis = inputForShare(sender, recipient, "missing.txt", new byte[]{1});
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);

        svc.share(dis, dos);

        DataInputStream resp = new DataInputStream(new ByteArrayInputStream(out.toByteArray()));
        assertEquals("ERR\tFILE_NOT_FOUND", resp.readUTF());
    }

    private static User user(String login, int id) {
        User u = new User();
        u.setLogin(login);
        u.setId(id);
        return u;
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

    private static DataInputStream inputForShare(
            String sender,
            String recipient,
            String filename,
            byte[] wrappedKey
    ) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buf);
        out.writeUTF(sender);
        out.writeUTF(recipient);
        out.writeUTF(filename);
        out.writeInt(wrappedKey.length);
        out.write(wrappedKey);
        out.flush();
        return new DataInputStream(new ByteArrayInputStream(buf.toByteArray()));
    }
}
