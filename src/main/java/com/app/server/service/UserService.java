package com.app.server.service;

import com.app.server.Config;
import com.app.server.model.User;
import com.app.server.repository.FileRepository;
import com.app.server.repository.UserRepository;
import com.app.server.utils.AppLogger;
import com.app.server.utils.Security;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Server-side service handling user-related commands.
 *
 * <p>The service reads request parameters from {@link DataInputStream} and writes protocol responses
 * to {@link DataOutputStream}. Persistence is delegated to {@link UserRepository}.
 */
public class UserService {
    private final UserRepository userRepo;
    private final FileRepository fileRepo;

    /**
     * Creates a service using default repositories.
     */
    public UserService() {
        this(new UserRepository(), new FileRepository());
    }

    /**
     * Package-private constructor for dependency injection (e.g. unit tests).
     *
     * @param userRepo user repository
     * @param fileRepo file repository
     */
    UserService(UserRepository userRepo, FileRepository fileRepo) {
        this.userRepo = userRepo;
        this.fileRepo = fileRepo;
    }


    /**
     * Handles the LOGIN command.
     *
     * <p>Request: {@code [login:String, password:String]}
     * <br>Response: {@code "OK" + role} or {@code "ERR\tINVALID_CREDENTIALS"}
     *
     * @param dis input stream
     * @param dos output stream
     * @throws IOException when protocol I/O fails
     */
    public void login(DataInputStream dis, DataOutputStream dos) throws IOException {
        String login         = dis.readUTF();
        String clientpassword    = dis.readUTF();

        Optional<String> role = userRepo.findByLogin(login)
                .filter(u -> {
                    boolean ok = Security.verifyPassword(clientpassword,u.getPasswordHash());

                    if (!ok) AppLogger.warn("Bad hash for login: " + login);
                    return ok;
                })
                .map(User::getRole);

        if (role.isPresent()) {
            dos.writeUTF("OK");
            dos.writeUTF(role.get());
        } else {
            dos.writeUTF("ERR\tINVALID_CREDENTIALS");
        }
    }

    /**
     * Handles the REGISTER command.
     *
     * <p>Request: {@code [login:String, fullName:String, passwordHash:String, email:String, plan:String]}
     * <br>Response: {@code "OK"} or {@code "ERR\t..."}.
     *
     * <p>Note: the {@code plan} parameter is currently accepted but ignored.
     *
     * @param dis input stream
     * @param dos output stream
     */
    public void register(DataInputStream dis, DataOutputStream dos) {
        try {
            String login     = dis.readUTF();
            String fullName  = dis.readUTF();
            String pwdHash   = dis.readUTF();
            String email     = dis.readUTF();
            String planIgnored = dis.readUTF();

            AppLogger.info("Register attempt: login=" + login + ", fullName=" + fullName);

            if (userRepo.existsLogin(login)) {
                dos.writeUTF("ERR\tUSER_EXISTS");
                dos.flush();
                return;
            }
            if (userRepo.existsEmail(email)) {
                dos.writeUTF("ERR\tEMAIL_EXISTS");
                dos.flush();
                return;
            }

            User u = new User();
            u.setLogin(login);
            u.setFullName(fullName);
            u.setPasswordHash(pwdHash);
            u.setEmail(email);
            u.setRole("USER");
            

            boolean saved = userRepo.save(u);
            if (saved) {
                createUserDirectory(login);
                dos.writeUTF("OK");
                dos.flush();
                AppLogger.info("User registered: " + login);
            } else {
                dos.writeUTF("ERR\tDB_ERROR");
                dos.flush();
            }

        } catch (IOException e ) {
            AppLogger.error("Registration failed", e);
            try {
                dos.writeUTF("ERR\tREGISTER_FAILED");
                dos.flush();
            } catch (IOException ioe) {
                AppLogger.error("Failed to report registration error", ioe);
            }
        }
    }
    /**
     * Handles CHECK_USER.
     *
     * <p>Request: {@code [login:String]}
     * <br>Response: {@code "true"} or {@code "false"}
     *
     * @param dis input stream
     * @param dos output stream
     * @throws IOException when protocol I/O fails
     */
    public void checkUser(DataInputStream dis, DataOutputStream dos) throws IOException {
        String login = dis.readUTF();
        boolean exists = userRepo.existsLogin(login);
        dos.writeUTF(Boolean.toString(exists));
    }

    /**
     * Handles CHECK_PASSWORD.
     *
     * <p>Request: {@code [login:String, password:String]}
     * <br>Response: {@code "OK"} or {@code "ERR"}
     *
     * @param dis input stream
     * @param dos output stream
     * @throws IOException when protocol I/O fails
     */
    public void checkPassword(DataInputStream dis, DataOutputStream dos) throws IOException {
        String login = dis.readUTF();
        String password = dis.readUTF();
        var opt = userRepo.findByLogin(login);
        if (opt.isEmpty()) {
            dos.writeUTF("ERR");
            return;
        }
        User u = opt.get();
        boolean ok = Security.verifyPassword(password, u.getPasswordHash());
        if (ok) {
            dos.writeUTF("OK");
        } else {
            dos.writeUTF("ERR");
        }
        dos.flush();
    }

    /**
     * Handles CHECK_EMAIL.
     *
     * <p>Request: {@code [email:String]}
     * <br>Response: {@code "true"} or {@code "false"}
     *
     * @param dis input stream
     * @param dos output stream
     * @throws IOException when protocol I/O fails
     */
    public void checkEmail(DataInputStream dis, DataOutputStream dos) throws IOException {
        String email = dis.readUTF();
        boolean exists = userRepo.existsEmail(email);
        dos.writeUTF(Boolean.toString(exists));
    }
    private void createUserDirectory(String login) throws IOException {
        Path userDir = Paths.get(Config.RECEIVED_FILES_PATH, login);
        Files.createDirectories(userDir);
    }


}
