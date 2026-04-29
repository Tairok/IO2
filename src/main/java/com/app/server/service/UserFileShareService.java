package com.app.server.service;



import com.app.server.Config;
import com.app.server.model.User;
import com.app.server.repository.FileRepository;
import com.app.server.repository.UserRepository;
import com.app.server.utils.AppLogger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.sql.SQLException;


/**
 * Server-side service responsible for sharing files between users.
 */
public class UserFileShareService {
    private final UserRepository userRepo;
    private final FileRepository fileRepo;

    /**
     * Creates a service using default repositories.
     */
    public UserFileShareService() {
        this(new UserRepository(), new FileRepository());
    }

    /**
     * Package-private constructor for dependency injection (e.g. unit tests).
     *
     * @param userRepo user repository
     * @param fileRepo file repository
     */
    UserFileShareService(UserRepository userRepo, FileRepository fileRepo) {
        this.userRepo = userRepo;
        this.fileRepo = fileRepo;
    }

    /**
     * Handles SHARE.
     *
     * <p>Request: {@code [sender:String, recipient:String, filename:String, wrappedKeyLength:int, wrappedKeyBytes...]}
     * <br>Response: {@code "OK"} or {@code "ERR\t..."}
     *
     * <p>The wrapped key bytes are read to keep the protocol aligned, but they are currently not
     * used by the server.
     *
     * @param dis input stream
     * @param dos output stream
     * @throws IOException when protocol I/O fails
     */
    public void share(DataInputStream dis, DataOutputStream dos) throws IOException {
        String sender    = dis.readUTF();
        String recipient = dis.readUTF();
        String filename  = dis.readUTF();
        readBytes(dis);

        AppLogger.info("SHARE: from=" + sender + " to=" + recipient + " file=" + filename);

        try {
            userRepo.findByLogin(sender)
                    .orElseThrow(() -> new IOException("ERR\tUNKNOWN_SENDER"));
            User uRecip  = userRepo.findByLogin(recipient)
                    .orElseThrow(() -> new IOException("ERR\tUNKNOWN_RECIPIENT"));

            Path src = Paths.get(Config.RECEIVED_FILES_PATH, sender, filename);
            if (!Files.exists(src)) throw new IOException("ERR\tFILE_NOT_FOUND");
            long size = Files.size(src);

            Path destDir = Paths.get(Config.RECEIVED_FILES_PATH, recipient);
            Files.createDirectories(destDir);
            Path dest = destDir.resolve(filename);

            Path tmpDest = destDir.resolve(filename + ".part");
            Files.copy(src, tmpDest, StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.move(tmpDest, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception ex) {
                Files.move(tmpDest, dest, StandardCopyOption.REPLACE_EXISTING);
            }

            fileRepo.save(uRecip, dest.toString(), size);

            AppLogger.info("Kept sender's original copy after share: " + filename);

            dos.writeUTF("OK");
            dos.flush();
            AppLogger.info("SHARE succeeded: " + filename);
        }
        catch (IOException e) {
            String msg = e.getMessage();
            AppLogger.error("Error in SHARE", e);
            
            dos.writeUTF(msg);
            dos.flush();
        }
    }

    private static byte[] readBytes(DataInputStream dis) throws IOException {
        int len = dis.readInt();
        byte[] data = new byte[len];
        dis.readFully(data);
        return data;
    }
}
