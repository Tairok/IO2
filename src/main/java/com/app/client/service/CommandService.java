package com.app.client.service;

import com.app.client.model.FileEntry;
import com.app.client.network.NetworkConnection;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Client-side command service that communicates with the server over a {@link NetworkConnection}.
 *
 * <p>Methods map to server protocol commands (e.g. LIST, UPLOAD, DOWNLOAD).
 */
public class CommandService {
    private final NetworkConnection conn;

    public CommandService(NetworkConnection conn) {
        this.conn = conn;
    }

    /**
     * Returns the output stream for writing protocol commands.
     *
     * @return data output stream bound to the current connection
     */
    public DataOutputStream getDos() {
        return conn.out();
    }

    /**
     * Returns the input stream for reading protocol responses.
     *
     * @return data input stream bound to the current connection
     */
    public DataInputStream getDis() {
        return conn.in();
    }

    /** LIST: retrieves user's file list */
    public List<FileEntry> list(String username) throws IOException {
        DataOutputStream dos = conn.out();
        DataInputStream  dis = conn.in();

        dos.writeUTF("LIST");
        dos.writeUTF(username);
        dos.flush();

        String status = dis.readUTF();
        if (!"OK".equals(status)) {
            throw new IOException("LIST failed: " + status);
        }

        int count = dis.readInt();
        List<FileEntry> files = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String[] parts = dis.readUTF().split("\t", 3);
            files.add(new FileEntry(parts[0],
                    Long.parseLong(parts[1]),
                    parts[2]));
        }
        return files;
    }

    /** DELETE: deletes file */
    public boolean delete(String username, String filename) throws IOException {
        DataOutputStream dos = conn.out();
        DataInputStream  dis = conn.in();

        dos.writeUTF("DELETE");
        dos.writeUTF(username);
        dos.writeUTF(filename);
        dos.flush();

        return "OK".equalsIgnoreCase(dis.readUTF());
    }


    /** QUERY: wykonuje SELECT, zwraca wiersze */
    public List<String[]> query(String sql) throws IOException {
        DataOutputStream dos = conn.out();
        DataInputStream  dis = conn.in();

        dos.writeUTF("QUERY");
        dos.writeUTF(sql);
        dos.flush();

        String status = dis.readUTF();
        if (!"OK".equals(status)) {
            throw new IOException("QUERY failed: " + status);
        }

        int colCount = dis.readInt();
        int rowCount = dis.readInt();
        List<String[]> rows = new ArrayList<>(rowCount);
        for (int i = 0; i < rowCount; i++) {
            String[] row = new String[colCount];
            for (int j = 0; j < colCount; j++) {
                row[j] = dis.readUTF();
            }
            rows.add(row);
        }
        return rows;
    }

    /** EXECUTE: INSERT/UPDATE/DELETE, zwraca liczbę zmienionych wierszy */
    public int executeUpdate(String sql) throws IOException {
        DataOutputStream dos = conn.out();
        DataInputStream  dis = conn.in();

        dos.writeUTF("EXECUTE");
        dos.writeUTF(sql);
        dos.flush();

        String status = dis.readUTF();
        if (!"OK".equals(status)) {
            throw new IOException("EXECUTE failed: " + status);
        }
        return dis.readInt();
    }

    /** LOGIN: authenticates and returns token / session ID */
    public String login(String username, String password) throws IOException {
        DataOutputStream dos = conn.out();
        DataInputStream  dis = conn.in();

        dos.writeUTF("LOGIN");
        dos.writeUTF(username);
        dos.writeUTF(password);
        dos.flush();

        String status = dis.readUTF();
        if (!"OK".equals(status)) return null;
        return dis.readUTF();
    }

    /** REGISTER: creates new user account */
    public boolean register(
            String login,
            String fullName,
            String pwdHash,
            String email,
            String plan
    ) throws IOException {
        DataOutputStream dos = conn.out();
        DataInputStream  dis = conn.in();

        dos.writeUTF("REGISTER");
        dos.writeUTF(login);
        dos.writeUTF(fullName);
        dos.writeUTF(pwdHash);
        dos.writeUTF(email);
        dos.writeUTF(plan);
        dos.flush();

        String status = dis.readUTF();
        return "OK".equalsIgnoreCase(status.trim());
    }

    /** CHECK_USER: czy login istnieje? */
    public boolean isUserExists(String login) throws IOException {
        DataOutputStream dos = conn.out();
        DataInputStream  dis = conn.in();

        dos.writeUTF("CHECK_USER");
        dos.writeUTF(login);
        dos.flush();

        return Boolean.parseBoolean(dis.readUTF());
    }

    /** CHECK_PASSWORD: validate a user's password (server-side). Returns true if password matches. */
    public boolean checkPassword(String login, String password) throws IOException {
        DataOutputStream dos = conn.out();
        DataInputStream  dis = conn.in();

        dos.writeUTF("CHECK_PASSWORD");
        dos.writeUTF(login);
        dos.writeUTF(password);
        dos.flush();

        String resp = dis.readUTF();
        return "OK".equalsIgnoreCase(resp);
    }

    /** CHECK_EMAIL: czy email zarejestrowany? */
    public boolean isEmailExists(String email) throws IOException {
        DataOutputStream dos = conn.out();
        DataInputStream  dis = conn.in();

        dos.writeUTF("CHECK_EMAIL");
        dos.writeUTF(email);
        dos.flush();

        return Boolean.parseBoolean(dis.readUTF());
    }

    /** SHARE: file sharing (E2E) */
    public boolean share(String sender, String recipient, String filename, byte[] wrappedKey) throws IOException {
        DataOutputStream dos = conn.out();
        DataInputStream  dis = conn.in();

        dos.writeUTF("SHARE");
        dos.writeUTF(sender);
        dos.writeUTF(recipient);
        dos.writeUTF(filename);
        writeBytes(dos, wrappedKey == null ? new byte[0] : wrappedKey);
        dos.flush();

        return "OK".equalsIgnoreCase(dis.readUTF());
    }

    public boolean share(String sender, String recipient, String filename) throws IOException {
        return share(sender, recipient, filename, new byte[0]);
    }

    public void storeUserKeys(String username,
                              String publicKey,
                              byte[] encryptedPrivateKey,
                              byte[] salt,
                              byte[] iv,
                              int iterations) throws IOException {
        DataOutputStream dos = conn.out();
        DataInputStream dis = conn.in();

        dos.writeUTF("STORE_USER_KEYS");
        dos.writeUTF(username);
        dos.writeUTF(publicKey);
        dos.writeInt(iterations);
        writeBytes(dos, salt);
        writeBytes(dos, iv);
        writeBytes(dos, encryptedPrivateKey);
        dos.flush();

        String resp = dis.readUTF();
        if (!"OK".equalsIgnoreCase(resp)) {
            throw new IOException("STORE_USER_KEYS failed: " + resp);
        }
    }

    public String getPublicKey(String username) throws IOException {
        DataOutputStream dos = conn.out();
        DataInputStream dis = conn.in();

        dos.writeUTF("GET_PUBLIC_KEY");
        dos.writeUTF(username);
        dos.flush();

        String status = dis.readUTF();
        if ("ERR\tNOT_FOUND".equalsIgnoreCase(status)) return null;
        if (!"OK".equalsIgnoreCase(status)) {
            throw new IOException("GET_PUBLIC_KEY failed: " + status);
        }
        return dis.readUTF();
    }

    private static void writeBytes(DataOutputStream dos, byte[] data) throws IOException {
        dos.writeInt(data.length);
        dos.write(data);
    }

    private static byte[] readBytes(DataInputStream dis) throws IOException {
        int len = dis.readInt();
        byte[] data = new byte[len];
        dis.readFully(data);
        return data;
    }
}
