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


public class UserFileShareService {
    private final UserRepository userRepo = new UserRepository();
    private final FileRepository fileRepo = new FileRepository();

    /**
     * SHARE: sender → recipient filename
     * Responds: OK or ERR\tCODE
     */
    public void share(DataInputStream dis, DataOutputStream dos) throws IOException {
        String sender    = dis.readUTF();
        String recipient = dis.readUTF();
        String filename  = dis.readUTF();

        AppLogger.info("SHARE: from=" + sender + " to=" + recipient + " file=" + filename);

        try {
            // 1) Get sender and recipient
            User uSender = userRepo.findByLogin(sender)
                    .orElseThrow(() -> new IOException("ERR\tUNKNOWN_SENDER"));
            User uRecip  = userRepo.findByLogin(recipient)
                    .orElseThrow(() -> new IOException("ERR\tUNKNOWN_RECIPIENT"));

            // 2) Check recipient limit


            // 3) File paths
            Path src = Paths.get(Config.RECEIVED_FILES_PATH, sender, filename);
            if (!Files.exists(src)) throw new IOException("ERR\tFILE_NOT_FOUND");
            long size = Files.size(src);
            //if (used + size > quota) throw new IOException("ERR\tRECIPIENT_QUOTA_EXCEEDED");

            // 4) Prepare recipient directory
            Path destDir = Paths.get(Config.RECEIVED_FILES_PATH, recipient);
            Files.createDirectories(destDir);
            Path dest = destDir.resolve(filename);

            // 5) Copy data
            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);

            // 6) Save metadata
            int dot = filename.lastIndexOf('.');
            if (dot >= 0 && dot < filename.length()-1) {

            }
            fileRepo.save(uRecip, dest.toString(), size);

            // 7) OK
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
}