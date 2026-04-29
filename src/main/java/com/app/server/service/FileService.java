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


/**
 * Server-side service responsible for file operations.
 *
 * <p>The service uses a simple binary protocol over {@link DataInputStream}/{@link DataOutputStream}
 * and persists file metadata via repositories.
 */
public class FileService {
    private static final DateTimeFormatter fmt =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserRepository userRepository;
    private final FileRepository fileRepository;

    /**
     * Creates a service using default repositories.
     */
    public FileService() {
        this(new UserRepository(), new FileRepository());
    }

    /**
     * Package-private constructor for dependency injection (e.g. unit tests).
     *
     * @param userRepository user repository
     * @param fileRepository file repository
     */
    FileService(UserRepository userRepository, FileRepository fileRepository) {
        this.userRepository = userRepository;
        this.fileRepository = fileRepository;
    }

    /**
     * Handles LIST.
     *
     * <p>Request: {@code [user:String]}
     * <br>Response: {@code "OK" + count:int + (name\tsize\tmodified)*}
     *
     * <p>Temporary files with {@code .part} extension are omitted. If a file has a sibling
     * {@code <name>.part}, it is also omitted (upload in progress).
     *
     * @param dis input stream
     * @param dos output stream
     * @throws IOException when protocol I/O fails
     */
    public void list(DataInputStream dis, DataOutputStream dos) throws IOException {
        String user = dis.readUTF();
        File dir = new File(Config.RECEIVED_FILES_PATH, user);
        File[] files = dir.isDirectory() ? dir.listFiles() : new File[0];

        List<FileInfo> infos = new ArrayList<>();
        if (files != null) {
            java.util.Set<String> names = new java.util.HashSet<>();
            for (File f : files) names.add(f.getName());

            for (File f : files) {
                String nm = f.getName();
                if (nm.endsWith(".part")) continue;
                if (names.contains(nm + ".part")) continue;

                LocalDateTime lm = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(f.lastModified()), ZoneId.systemDefault());
                infos.add(new FileInfo(nm, f.length(), lm));
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

    /**
     * Handles UPLOAD.
     *
     * <p>Request: {@code [login:String, filename:String, length:long, bytes...]}
     * <br>Response: {@code "OK"} (ready) then {@code "OK"} (completed)
     *
     * <p>The server writes to {@code <filename>.part} first and then moves it to the final name.
     * File metadata is persisted after a successful transfer.
     *
     * @param dis input stream
     * @param dos output stream
     * @throws IOException when protocol I/O fails
     * @throws SQLException when repository layer throws SQL errors
     */
    public void upload(DataInputStream dis, DataOutputStream dos)
            throws IOException, SQLException
    {
        String login    = dis.readUTF();
        String filename = dis.readUTF();
        long   length   = dis.readLong();

        AppLogger.info("UPLOAD request: user=" + login + ", file=" + filename + ", size=" + length);


        User u = userRepository.findByLogin(login)
                .orElseThrow(() -> new IOException("Unknown user: " + login));
        Path userDir = Paths.get(Config.RECEIVED_FILES_PATH, login);
        Files.createDirectories(userDir);

        dos.writeUTF("OK");
        dos.flush();


        Path outFile = userDir.resolve(filename);
        Path tmpFile = userDir.resolve(filename + ".part");
        try (var fos = Files.newOutputStream(tmpFile,
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
            fos.flush();
        }

        try {
            Files.move(tmpFile, outFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception ex) {
            Files.move(tmpFile, outFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        dos.writeUTF("OK");
        dos.flush();

        fileRepository.save(u, filename, length);

        AppLogger.info("UPLOAD succeeded and metadata saved: " +
                filename + " (" + length + " bytes)");
    }



    /**
     * Handles DOWNLOAD.
     *
     * <p>Request: {@code [user:String, filename:String]}
     * <br>Response: {@code length:long + bytes...} or {@code -1L} when missing.
     *
     * @param dis input stream
     * @param dos output stream
     */
    public void download(DataInputStream dis, DataOutputStream dos) {
        try {
            String user = dis.readUTF();
            String name = dis.readUTF();

            File f = new File(new File(Config.RECEIVED_FILES_PATH, user), name);
            if (!f.exists() || !f.isFile()) {
                dos.writeLong(-1L);
                dos.flush();
                AppLogger.warn("DOWNLOAD: file not found for " + user + "/" + name);
                return;
            }

            long size = f.length();
            dos.writeLong(size);

            try (InputStream fis = new FileInputStream(f)) {
                byte[] buf = new byte[8 * 1024];
                int read;
                while ((read = fis.read(buf)) != -1) {
                    dos.write(buf, 0, read);
                }
            }

            dos.flush();
            AppLogger.info("DOWNLOAD: sent " + size + " bytes for " + user + "/" + name);

        } catch (EOFException eof) {
            AppLogger.warn("DOWNLOAD: client disconnected early");
        } catch (IOException ioe) {
            AppLogger.error("DOWNLOAD: I/O error", ioe);
        }
    }


    /**
     * Handles DELETE.
     *
     * <p>Request: {@code [login:String, filename:String]}
     * <br>Response: {@code "OK"} or {@code "ERR\tDELETE_FAILED"}.
     *
     * <p>The operation is treated as successful if either the on-disk file is removed or the
     * metadata row is removed.
     *
     * @param dis input stream
     * @param dos output stream
     * @throws IOException when protocol I/O fails
     */
    public void delete(DataInputStream dis, DataOutputStream dos) throws IOException {
        String login = dis.readUTF();
        String name  = dis.readUTF();

        User u = userRepository.findByLogin(login)
                .orElseThrow(() -> new IOException("Unknown user: " + login));

        File diskFile = new File(new File(Config.RECEIVED_FILES_PATH, login), name);
        boolean fsOk = diskFile.delete();

        boolean metaOk = false;
        try {
            metaOk = fileRepository.deleteMetadata(u, name);
        } catch (Exception e) {
            AppLogger.error("Error deleting metadata for " + name, e);
        }

        if (fsOk || metaOk) {
            dos.writeUTF("OK");
        } else {
            dos.writeUTF("ERR\tDELETE_FAILED");
        }
        dos.flush();
    }


    /**
     * Handles CREATE.
     *
     * <p>Request: {@code [user:String, filename:String]}
     * <br>Response: {@code "OK"} or {@code "ERR\tCREATE_FAILED"}.
     *
     * @param dis input stream
     * @param dos output stream
     * @throws IOException when protocol I/O fails
     */
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
