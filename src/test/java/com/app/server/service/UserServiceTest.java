package com.app.server.service;

import com.app.client.utils.Security;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    private static class StubUserRepository extends UserRepository {
        Optional<User> userByLogin = Optional.empty();
        boolean loginExists;
        boolean emailExists;
        boolean saveResult = true;
        User savedUser;

        @Override
        public Optional<User> findByLogin(String login) {
            return userByLogin;
        }

        @Override
        public boolean existsLogin(String login) {
            return loginExists;
        }

        @Override
        public boolean existsEmail(String email) {
            return emailExists;
        }

        @Override
        public boolean save(User u) {
            savedUser = u;
            return saveResult;
        }
    }

    @Test
    void loginReturnsOkAndRoleWhenPasswordMatches() throws Exception {
        StubUserRepository repo = new StubUserRepository();
        User u = new User();
        u.setLogin("alice");
        u.setRole("ADMIN");
        u.setPasswordHash(Security.hashPassword("secret"));
        repo.userByLogin = Optional.of(u);

        UserService svc = new UserService(repo, new FileRepository());

        DataInputStream dis = inputWithUtf("alice", "secret");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);

        svc.login(dis, dos);

        DataInputStream resp = new DataInputStream(new ByteArrayInputStream(out.toByteArray()));
        assertEquals("OK", resp.readUTF());
        assertEquals("ADMIN", resp.readUTF());
    }

    @Test
    void loginReturnsInvalidCredentialsWhenPasswordIsWrong() throws Exception {
        StubUserRepository repo = new StubUserRepository();
        User u = new User();
        u.setLogin("alice");
        u.setRole("USER");
        u.setPasswordHash(Security.hashPassword("secret"));
        repo.userByLogin = Optional.of(u);

        UserService svc = new UserService(repo, new FileRepository());

        DataInputStream dis = inputWithUtf("alice", "wrong");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);

        svc.login(dis, dos);

        DataInputStream resp = new DataInputStream(new ByteArrayInputStream(out.toByteArray()));
        assertEquals("ERR\tINVALID_CREDENTIALS", resp.readUTF());
    }

    @Test
    void checkPasswordReturnsOkWhenPasswordMatches() throws Exception {
        StubUserRepository repo = new StubUserRepository();
        User u = new User();
        u.setLogin("alice");
        u.setPasswordHash(Security.hashPassword("secret"));
        repo.userByLogin = Optional.of(u);

        UserService svc = new UserService(repo, new FileRepository());

        DataInputStream dis = inputWithUtf("alice", "secret");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);

        svc.checkPassword(dis, dos);

        DataInputStream resp = new DataInputStream(new ByteArrayInputStream(out.toByteArray()));
        assertEquals("OK", resp.readUTF());
    }

    @Test
    void registerRejectsDuplicateLogin() throws Exception {
        StubUserRepository repo = new StubUserRepository();
        repo.loginExists = true;

        UserService svc = new UserService(repo, new FileRepository());

        DataInputStream dis = inputWithUtf(
                "newUser",
                "New User",
                "hash",
                "user@example.com",
                "FREE"
        );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);

        svc.register(dis, dos);

        DataInputStream resp = new DataInputStream(new ByteArrayInputStream(out.toByteArray()));
        assertEquals("ERR\tUSER_EXISTS", resp.readUTF());
        assertNull(repo.savedUser);
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        StubUserRepository repo = new StubUserRepository();
        repo.emailExists = true;

        UserService svc = new UserService(repo, new FileRepository());

        DataInputStream dis = inputWithUtf(
                "newUser",
                "New User",
                "hash",
                "user@example.com",
                "FREE"
        );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);

        svc.register(dis, dos);

        DataInputStream resp = new DataInputStream(new ByteArrayInputStream(out.toByteArray()));
        assertEquals("ERR\tEMAIL_EXISTS", resp.readUTF());
        assertNull(repo.savedUser);
    }

    @Test
    void registerCreatesUserDirectoryAndSavesUser() throws Exception {
        String login = "testuser_" + System.nanoTime();
        StubUserRepository repo = new StubUserRepository();

        UserService svc = new UserService(repo, new FileRepository());

        Path userDir = Paths.get(Config.RECEIVED_FILES_PATH, login);
        deleteRecursively(userDir);

        DataInputStream dis = inputWithUtf(
                login,
                "Test User",
                "hash",
                "user_" + login + "@example.com",
                "FREE"
        );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);

        svc.register(dis, dos);

        DataInputStream resp = new DataInputStream(new ByteArrayInputStream(out.toByteArray()));
        assertEquals("OK", resp.readUTF());

        assertNotNull(repo.savedUser);
        assertEquals(login, repo.savedUser.getLogin());
        assertEquals("USER", repo.savedUser.getRole());
        assertTrue(Files.isDirectory(userDir));

        deleteRecursively(userDir);
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

    private static DataInputStream inputWithUtf(String... values) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buf);
        for (String value : values) {
            out.writeUTF(value);
        }
        out.flush();
        return new DataInputStream(new ByteArrayInputStream(buf.toByteArray()));
    }
}
