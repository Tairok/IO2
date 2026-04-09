// src/main/java/com/capp/server/service/UserService.java
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
import java.sql.SQLException;
import java.util.Optional;

public class UserService {
    private final UserRepository userRepo = new UserRepository();
    private final FileRepository fileRepo = new FileRepository();


    /**
     * Handles the LOGIN command.
     * Expects: [ login:String, passwordHash:String ]
     * Replies: "OK" + role  or  "ERR\tINVALID_CREDENTIALS"
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
     * Expects: [ login, passwordHash, username, email, firstName, lastName,
     *            address, city, postalCode ]
     * Replies: "OK" or "ERR\tREGISTER_FAILED"
     */
    public void register(DataInputStream dis, DataOutputStream dos) {
        try {
            String login     = dis.readUTF();
            String fullName  = dis.readUTF();
            String pwdHash   = dis.readUTF();
            String email     = dis.readUTF();


            AppLogger.info("Register attempt: login=" + login + ", fullName=" + fullName);

            // 1) unikalność loginu
            if (userRepo.existsLogin(login)) {
                dos.writeUTF("ERR\tUSER_EXISTS");
                dos.flush();
                return;
            }
            // 2) unikalność e-maila
            if (userRepo.existsEmail(email)) {
                dos.writeUTF("ERR\tEMAIL_EXISTS");
                dos.flush();
                return;
            }

            // 3) create object and save
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
     * CMD CHECK_USER:
     * Expects: [ login ]
     * Replies: "true" or "false"
     */
    public void checkUser(DataInputStream dis, DataOutputStream dos) throws IOException {
        String login = dis.readUTF();
        boolean exists = userRepo.existsLogin(login);
        dos.writeUTF(Boolean.toString(exists));
    }

    /**
     * CMD CHECK_EMAIL:
     * Expects: [ email ]
     * Replies: "true" or "false"
     */
    public void checkEmail(DataInputStream dis, DataOutputStream dos) throws IOException {
        String email = dis.readUTF();
        boolean exists = userRepo.existsEmail(email);
        dos.writeUTF(Boolean.toString(exists));
    }
    /*
    public void sendUsage(DataInputStream dis, DataOutputStream dos) throws IOException, SQLException {
        String login = dis.readUTF();
        System.out.println("[SERVER] Computing usage for: " + login); // DEBUG

        
        dos.writeUTF("OK");
        
    }*/
    /*
    public void sendUsageWithQuota(DataInputStream dis, DataOutputStream dos) throws IOException {
        String login = dis.readUTF();
        Optional<UserRepository.UsageInfo> opt = userRepo.getUsageInfo(login);
        if (opt.isEmpty()) {
            dos.writeUTF("ERR\tNO_USER");
            return;
        }
        var info = userRepo.getUsageInfo(login).get();
        dos.writeUTF("OK");
        
        dos.writeLong(info.storageLimitGb() * Config.BYTES_PER_GB);

    }*/

    private void createUserDirectory(String login) throws IOException {
        Path userDir = Paths.get(Config.RECEIVED_FILES_PATH, login);
        Files.createDirectories(userDir);
    }


}
