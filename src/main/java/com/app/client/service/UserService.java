// com/capp/client/admin/service/UserService.java
package com.app.client.service;

import com.app.client.model.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    private final CommandService svc;

    public UserService(CommandService svc) {
        this.svc = svc;
    }

    /** Retrieves all users from the server */
    public List<User> list() throws IOException {
        List<String[]> rows = svc.query(
                "SELECT id, login, password_hash, email, full_name, role FROM users ORDER BY id");
        List<User> users = new ArrayList<>(rows.size());
        for (String[] row : rows) {
            users.add(mapRow(row));
        }
        return users;
    }

    /** Creates a new user via EXECUTE command */
    public void create(User newUser) throws IOException {
        if (newUser == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        ensurePassword(newUser);
        String sql = String.format(
                "INSERT INTO users (login, password_hash, email, full_name, role) VALUES ('%s','%s','%s','%s','%s')",
                escape(newUser.getLogin()),
                escape(newUser.getPassword()),
                escape(newUser.getEmail()),
                escape(newUser.getFullName()),
                escape(newUser.getRole())
        );
        int affected = svc.executeUpdate(sql);
        if (affected == 0) {
            throw new IOException("No user was created");
        }
    }

    /** Updates user attributes via EXECUTE command */
    public void update(User user) throws IOException {
        if (user == null || user.getId() <= 0) {
            throw new IllegalArgumentException("User must have an id");
        }
        ensurePassword(user);
        String sql = String.format(
                "UPDATE users SET login='%s', password_hash='%s', email='%s', full_name='%s', role='%s' WHERE id=%d",
                escape(user.getLogin()),
                escape(user.getPassword()),
                escape(user.getEmail()),
                escape(user.getFullName()),
                escape(user.getRole()),
                user.getId()
        );
        int affected = svc.executeUpdate(sql);
        if (affected == 0) {
            throw new IOException("No user was updated");
        }
    }

    /** Deletes user with given ID */
    public void delete(int id) throws IOException {
        if (id <= 0) {
            throw new IllegalArgumentException("Invalid user id");
        }
        int affected = svc.executeUpdate("DELETE FROM users WHERE id = " + id);
        if (affected == 0) {
            throw new IOException("No user was deleted");
        }
    }

    /** Returns user id or -1 if the user does not exist */
    public int getUserIdByLogin(String login) throws IOException {
        List<String[]> rows = svc.query(
                "SELECT id FROM users WHERE login = '" + escape(login) + "'");
        if (rows.isEmpty() || rows.get(0).length == 0) {
            return -1;
        }
        try {
            return Integer.parseInt(rows.get(0)[0]);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid id format returned by server");
        }
    }

    /** wraps CommandService.isUserExists(...) */
    public boolean isUserExists(String login) throws IOException {
        return svc.isUserExists(login);
    }

    /** wraps CommandService.isEmailExists(...) */
    public boolean isEmailExists(String email) throws IOException {
        return svc.isEmailExists(email);
    }

    /** wraps CommandService.register(...) */
    public boolean register(
            String login,
            String fullName,
            String pwdHash,
            String email,
            String plan
    ) throws IOException {
        return svc.register(login, fullName, pwdHash, email, plan);
    }

    private User mapRow(String[] row) {
        User u = new User();
        if (row.length > 0 && !row[0].isEmpty()) {
            u.setId(Integer.parseInt(row[0]));
        }
        if (row.length > 1) u.setLogin(row[1]);
        if (row.length > 2) u.setPassword(row[2]);
        if (row.length > 3) u.setEmail(row[3]);
        if (row.length > 4) u.setFullName(row[4]);
        if (row.length > 5) u.setRole(row[5]);
        return u;
    }

    private void ensurePassword(User user) {
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be empty");
        }
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("'", "''");
    }
}
