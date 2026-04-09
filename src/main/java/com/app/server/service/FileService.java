package com.app.server.service;

import com.app.server.Config;
import com.app.server.model.FileInfo;
import com.app.server.model.User;
import com.app.server.repository.FileRepository;
import com.app.server.repository.UserRepository;
import com.app.server.utils.AppLogger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


public class FileService {
    private static final DateTimeFormatter fmt =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final UserRepository userRepository = new UserRepository();
    private final FileRepository fileRepository = new FileRepository();

    /** LIST */
    public void list(DataInputStream dis, DataOutputStream dos) throws IOException {
        String user = dis.readUTF();
        File dir = new File(Config.RECEIVED_FILES_PATH, user);
        File[] files = dir.isDirectory() ? dir.listFiles() : new File[0];

        List<FileInfo> infos = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                LocalDateTime lm = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(f.lastModified()), ZoneId.systemDefault());
                infos.add(new FileInfo(f.getName(), f.length(), lm));
            }
        }

        dos.writeUTF("OK");
        dos.writeInt(infos.size());
        for (FileInfo fi : infos) {
            dos.writeUTF(fi.name() + "\t" + fi.size() + "\t"
                    + fi.modified().format(fmt));
        }
        dos.flush();
    }

    /** UPLOAD */
    public void upload(DataInputStream dis, DataOutputStream dos)
            throws IOException, SQLException
    {
        // 1) metadata
        String login    = dis.readUTF();
        String filename = dis.readUTF();
        long   length   = dis.readLong();

        AppLogger.info("UPLOAD request: user=" + login + ", file=" + filename + ", size=" + length);

        // 2) get user
        User u = userRepository.findByLogin(login)
                .orElseThrow(() -> new IOException("Unknown user: " + login));

        // 3) calculate limit based on default plan


        // 4) calculate already used space
        
/*
        if (used + length > quotaBytes) {
            dos.writeUTF("ERR\tQUOTA_EXCEEDED");
            dos.flush();
            return;
        }*/

        // 5) prepare user directory
        Path userDir = Paths.get(Config.RECEIVED_FILES_PATH, login);
        Files.createDirectories(userDir);

        // 6) confirm readiness for sending
        dos.writeUTF("OK");
        dos.flush();

        // 7) receive byte stream
        Path outFile = userDir.resolve(filename);
        try (var fos = Files.newOutputStream(outFile,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))
        {
            byte[] buf = new byte[4096];
            long remaining = length;
            while (remaining > 0) {
                int toRead = (int) Math.min(buf.length, remaining);
                int r = dis.read(buf, 0, toRead);
                if (r < 0) throw new EOFException("Stream ended early");
                fos.write(buf, 0, r);
                remaining -= r;
            }
        }

        // 8) potwierdzenie zakończenia
        dos.writeUTF("OK");
        dos.flush();

        // 9) save metadata in database
        fileRepository.save(u, filename, length);

        AppLogger.info("UPLOAD succeeded and metadata saved: " +
                filename + " (" + length + " bytes)");
    }



    /** DOWNLOAD */
    public void download(DataInputStream dis, DataOutputStream dos) {
        try {
            // 1) Read user & filename
            String user = dis.readUTF();
            String name = dis.readUTF();

            // 2) Locate the file
            File f = new File(new File(Config.RECEIVED_FILES_PATH, user), name);
            if (!f.exists() || !f.isFile()) {
                // signal “not found”
                dos.writeLong(-1L);
                dos.flush();
                AppLogger.warn("DOWNLOAD: file not found for " + user + "/" + name);
                return;
            }

            // 3) Send the length prefix
            long size = f.length();
            dos.writeLong(size);

            // 4) Stream exactly `size` bytes
            try (InputStream fis = new FileInputStream(f)) {
                byte[] buf = new byte[8 * 1024];
                int read;
                while ((read = fis.read(buf)) != -1) {
                    dos.write(buf, 0, read);
                }
            }

            // 5) Flush and finish
            dos.flush();
            AppLogger.info("DOWNLOAD: sent " + size + " bytes for " + user + "/" + name);

        } catch (EOFException eof) {
            // client closed prematurely—just log and return, do NOT rethrow
            AppLogger.warn("DOWNLOAD: client disconnected early");
        } catch (IOException ioe) {
            // some other I/O problem
            AppLogger.error("DOWNLOAD: I/O error", ioe);
        }
    }


    /** DELETE */
    public void delete(DataInputStream dis, DataOutputStream dos) throws IOException {
        String login = dis.readUTF();
        String name  = dis.readUTF();

        // 1) get user
        User u = userRepository.findByLogin(login)
                .orElseThrow(() -> new IOException("Unknown user: " + login));

        // 2) delete file from disk
        File diskFile = new File(new File(Config.RECEIVED_FILES_PATH, login), name);
        boolean fsOk = diskFile.delete();

        boolean metaOk = false;
        try {
            metaOk = fileRepository.deleteMetadata(u, name);
        } catch (Exception e) {
            AppLogger.error("Error deleting metadata for " + name, e);
        }

        // Consider deletion successful if either the file was removed from disk
        // or metadata was removed from the database. This makes delete more robust
        // in face of transient DB inconsistencies.
        if (fsOk || metaOk) {
            dos.writeUTF("OK");
        } else {
            dos.writeUTF("ERR\tDELETE_FAILED");
        }
        dos.flush();
    }


    /** CREATE */
    public void create(DataInputStream dis, DataOutputStream dos) throws IOException {
        String user = dis.readUTF();
        String name = dis.readUTF();
        File f = new File(new File(Config.RECEIVED_FILES_PATH, user), name);
        f.getParentFile().mkdirs();
        boolean ok = f.createNewFile();
        dos.writeUTF(ok ? "OK" : "ERR\tCREATE_FAILED");
        dos.flush();
    }




}
