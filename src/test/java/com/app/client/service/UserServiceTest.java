package com.app.client.service;

import com.app.client.model.User;
import com.app.client.network.NetworkConnection;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    private static class FakeCommandService extends CommandService {
        String lastSql;
        List<String[]> queryRows = new ArrayList<>();
        int executeUpdateResult = 1;
        boolean userExists;
        boolean emailExists;
        boolean registerResult = true;

        FakeCommandService() {
            super(new NetworkConnection());
        }

        @Override
        public List<String[]> query(String sql) {
            lastSql = sql;
            return queryRows;
        }

        @Override
        public int executeUpdate(String sql) {
            lastSql = sql;
            return executeUpdateResult;
        }

        @Override
        public boolean isUserExists(String login) {
            return userExists;
        }

        @Override
        public boolean isEmailExists(String email) {
            return emailExists;
        }

        @Override
        public boolean register(String login, String fullName, String pwdHash, String email, String plan) {
            return registerResult;
        }
    }

    @Test
    void createEscapesSingleQuotes() throws IOException {
        FakeCommandService fake = new FakeCommandService();
        UserService svc = new UserService(fake);

        User u = new User();
        u.setLogin("o'neil");
        u.setPassword("hash");
        u.setEmail("o'neil@example.com");
        u.setFullName("O'Neil User");
        u.setRole("USER");

        svc.create(u);
        assertNotNull(fake.lastSql);
        assertTrue(fake.lastSql.contains("o''neil"));
        assertTrue(fake.lastSql.contains("O''Neil User"));
    }

    @Test
    void createThrowsOnBlankPassword() {
        FakeCommandService fake = new FakeCommandService();
        UserService svc = new UserService(fake);

        User u = new User();
        u.setLogin("user");
        u.setPassword("  ");

        assertThrows(IllegalArgumentException.class, () -> svc.create(u));
    }

    @Test
    void updateThrowsOnMissingId() {
        FakeCommandService fake = new FakeCommandService();
        UserService svc = new UserService(fake);

        User u = new User();
        u.setId(0);
        u.setPassword("hash");

        assertThrows(IllegalArgumentException.class, () -> svc.update(u));
    }

    @Test
    void deleteThrowsOnInvalidId() {
        FakeCommandService fake = new FakeCommandService();
        UserService svc = new UserService(fake);

        assertThrows(IllegalArgumentException.class, () -> svc.delete(0));
    }

    @Test
    void getUserIdByLoginReturnsMinusOneWhenEmpty() throws IOException {
        FakeCommandService fake = new FakeCommandService();
        UserService svc = new UserService(fake);

        int id = svc.getUserIdByLogin("missing");
        assertEquals(-1, id);
    }

    @Test
    void getUserIdByLoginThrowsOnInvalidNumber() {
        FakeCommandService fake = new FakeCommandService();
        fake.queryRows = new ArrayList<>();
fake.queryRows.add(new String[] { "abc" });
        UserService svc = new UserService(fake);

        assertThrows(IOException.class, () -> svc.getUserIdByLogin("bad"));
    }
}
