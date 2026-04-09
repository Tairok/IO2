// src/main/java/com/capp/client/service/CommandService.java
package com.app.client.service;

import com.app.client.model.FileEntry;
import com.app.client.network.NetworkConnection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CommandService {
    private final NetworkConnection conn;

    public CommandService(NetworkConnection conn) {
        this.conn = conn;
    }

    /** For raw socket protocols (invoices, payments, etc.) */
    public DataOutputStream getDos() {
        return conn.out();
    }
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

    /** CHECK_EMAIL: czy email zarejestrowany? */
    public boolean isEmailExists(String email) throws IOException {
        DataOutputStream dos = conn.out();
        DataInputStream  dis = conn.in();

        dos.writeUTF("CHECK_EMAIL");
        dos.writeUTF(email);
        dos.flush();

        return Boolean.parseBoolean(dis.readUTF());
    }

    // share method removed
}
