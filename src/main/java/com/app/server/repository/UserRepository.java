package com.app.server.repository;

import com.app.server.model.User;
import com.app.server.utils.AppLogger;
import com.app.server.service.DbService;
import com.app.server.utils.IdGenerator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UserRepository {
    private static final String SELECT_BY_LOGIN =
            "SELECT id, login, password_hash, email, full_name, role " +
                    "FROM users WHERE login = ?";

    private static final String SELECT_BY_EMAIL =
            "SELECT id, login, password_hash, email, full_name, role " +
                    "FROM users WHERE email = ?";

    private static final String INSERT_USER =
            "INSERT INTO users (id, login, password_hash, email, full_name, role) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

    public Optional<User> findByLogin(String login) {
        try (ResultSet rs = DbService.executeQuery(SELECT_BY_LOGIN, login)) {
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            AppLogger.error("Error fetching user by login: " + login, e);
        }
        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) {
        try (ResultSet rs = DbService.executeQuery(SELECT_BY_EMAIL, email)) {
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            AppLogger.error("Error fetching user by email: " + email, e);
        }
        return Optional.empty();
    }

    public boolean existsLogin(String login) {
        return findByLogin(login).isPresent();
    }

    public boolean existsEmail(String email) {
        return findByEmail(email).isPresent();
    }

    public boolean save(User u) {
        try {
            int rows = DbService.executeUpdate(
                    INSERT_USER,
                    IdGenerator.nextId("users"),
                    u.getLogin(),
                    u.getPasswordHash(),
                    u.getEmail(),
                    u.getFullName(),
                    u.getRole()
                   // u.getUsedBytes()        // domyślnie 0, ale możesz nadpisać
            );
            return rows == 1;
        } catch (SQLException e) {
            AppLogger.error("Error inserting user: " + u.getLogin(), e);
            return false;
        }
    }

    private Optional<User> mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setLogin(rs.getString("login"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setEmail(rs.getString("email"));
        u.setFullName(rs.getString("full_name"));
        u.setRole(rs.getString("role"));
        
        return Optional.of(u);
    }

    /** Simple helper class for reporting usage vs limit */
    //public record UsageInfo(int storageLimitGb) {}

    /**
     * Retrieves the sum of file sizes for the user and the default GB limit.
     */
    /*
    public Optional<UsageInfo> getUsageInfo(String login) {
        String sql =
                "SELECT " +
                        "  " +
                        "  10 AS storage_limit_gb " +
                        "FROM users u " +
                        "LEFT JOIN files f ON f.owner_id = u.id " +
                        "WHERE u.login = ? " +
                        "GROUP BY u.id";

        try (ResultSet rs = DbService.executeQuery(sql, login)) {
            if (rs.next()) {
                return Optional.of(new UsageInfo(
                        
                        rs.getInt("storage_limit_gb")
                ));
            }
        } catch (SQLException e) {
            AppLogger.error("Failed to fetch usage info for: " + login, e);
        }
        return Optional.empty();
    }*/


}
