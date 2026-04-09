package com.app.server.network;

import com.app.server.Config;
import com.app.server.utils.AppLogger;
import com.app.server.transfer.FileReceiver;

import java.io.*;
import java.net.Socket;

public class ConnectionHandler implements Runnable {
    private final Socket socket;

    public ConnectionHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        AppLogger.debug("Handler started for " + socket.getRemoteSocketAddress());

        try (DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            while (!socket.isClosed()) {
                String cmd;

                try {
                    // Odczyt komendy
                    cmd = dis.readUTF();
                } catch (EOFException eof) {
                    AppLogger.warn("Client " + socket.getRemoteSocketAddress() + " disconnected without sending command.");
                    break;
                } catch (IOException e) {
                    AppLogger.error("Error reading command from client " + socket.getRemoteSocketAddress(), e);
                    break;
                }

                if (cmd == null) {
                    AppLogger.warn("Received empty command from client.");
                    continue;
                }

                AppLogger.debug("Command from client: " + cmd);

                switch (cmd) {
                    case "PING" -> {
                        dos.writeUTF("PONG");
                        dos.flush();
                        AppLogger.info("Replied PONG to " + socket.getRemoteSocketAddress());
                    }
                    case "SEND_FILE" -> {
                        AppLogger.info("Starting file reception from " + socket.getRemoteSocketAddress());
                        try {
                            FileReceiver.receive(dis, Config.RECEIVED_FILES_PATH);
                        } catch (IOException e) {
                            AppLogger.error("Error during file reception", e);
                        }
                    }
                    case "LIST" -> {
                        String username = dis.readUTF();
                        File userDir = new File(Config.RECEIVED_FILES_PATH, username);

                        if (!userDir.exists() || !userDir.isDirectory()) {
                            dos.writeUTF("ERROR");
                            dos.flush();
                            AppLogger.warn("User directory missing: " + username);
                            break;
                        }

                        File[] files = userDir.listFiles(File::isFile);
                        if (files == null) {
                            dos.writeUTF("ERROR");
                            dos.flush();
                            AppLogger.warn("Cannot read files from directory: " + userDir);
                            break;
                        }

                        dos.writeUTF("OK");
                        dos.writeInt(files.length);

                        var sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        for (File file : files) {
                            String line = file.getName() + "\t" + file.length() + "\t" + sdf.format(file.lastModified());
                            dos.writeUTF(line);
                        }
                        dos.flush();

                        AppLogger.info("Sent list of " + files.length + " files for user " + username);
                    }
                    case "QUIT" -> {
                        AppLogger.info("Connection closed by client: " + socket.getRemoteSocketAddress());
                        return;
                    }

                    case "UPLOAD" -> {
                        String username = dis.readUTF();
                        String filename = dis.readUTF();
                        long fileLength = dis.readLong();

                        File userDir = new File(Config.RECEIVED_FILES_PATH, username);
                        if (!userDir.exists()) {
                            userDir.mkdirs();
                        }

                        File targetFile = new File(userDir, filename);

                        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                            byte[] buffer = new byte[4096];
                            long totalRead = 0;

                            while (totalRead < fileLength) {
                                int toRead = (int) Math.min(buffer.length, fileLength - totalRead);
                                int read = dis.read(buffer, 0, toRead);
                                if (read == -1) break;
                                fos.write(buffer, 0, read);
                                totalRead += read;
                            }
                            dos.writeUTF("OK");
                            AppLogger.info("File '" + filename + "' was saved for user: " + username);
                        } catch (IOException e) {
                            dos.writeUTF("ERROR");
                            AppLogger.error("Error during file save: " + targetFile.getAbsolutePath(), e);
                        }
                        dos.flush();
                    }
                    case "DOWNLOAD" -> {
                        String username = dis.readUTF();
                        String filename = dis.readUTF();

                        File userDir = new File(Config.RECEIVED_FILES_PATH, username);
                        File file = new File(userDir, filename);

                        if (!file.exists() || !file.isFile()) {
                            dos.writeLong(-1); // signal: no file
                            AppLogger.warn("File does not exist for download: " + file.getAbsolutePath());
                        } else {
                            dos.writeLong(file.length()); // first file length

                            try (FileInputStream fis = new FileInputStream(file)) {
                                byte[] buf = new byte[4096];
                                int read;
                                while ((read = fis.read(buf)) != -1) {
                                    dos.write(buf, 0, read);
                                }
                            }

                            dos.flush();
                            AppLogger.info("Sent file: " + file.getAbsolutePath());
                        }
                    }
                    case "DELETE" -> {
                        String username = dis.readUTF();
                        String filename = dis.readUTF();
                        File file = new File(Config.RECEIVED_FILES_PATH + File.separator + username, filename);

                        if (file.exists() && file.isFile()) {
                            boolean deleted = file.delete();
                            if (deleted) {
                                dos.writeUTF("OK");
                                AppLogger.info("Deleted file: " + file.getAbsolutePath());
                            } else {
                                dos.writeUTF("ERROR");
                                AppLogger.warn("Failed to delete file: " + file.getAbsolutePath());
                            }
                        } else {
                            dos.writeUTF("ERROR");
                            AppLogger.warn("File does not exist: " + file.getAbsolutePath());
                        }
                        dos.flush();
                    }
                    case "CREATE" -> {
                        String username = dis.readUTF();
                        String filename = dis.readUTF();
                        File userDir = new File(Config.RECEIVED_FILES_PATH, username);
                        if (!userDir.exists()) {
                            userDir.mkdirs();
                        }
                        File file = new File(userDir, filename);
                        boolean created = file.createNewFile();
                        if (created) {
                            dos.writeUTF("OK");
                            AppLogger.info("Created file: " + file.getAbsolutePath());
                        } else {
                            dos.writeUTF("ERROR");
                            AppLogger.warn("Failed to create file (already exists?): " + file.getAbsolutePath());
                        }
                        dos.flush();
                    }

                    default -> {
                        dos.writeUTF("ERROR: Unknown command");
                        dos.flush();
                        AppLogger.warn("Unknown command: " + cmd + " from client: " + socket.getRemoteSocketAddress());
                    }
                }

            }

        } catch (IOException e) {
            AppLogger.error("Connection error with client: " + socket.getRemoteSocketAddress(), e);
        } finally {
            try {
                socket.close();
                AppLogger.info("Socket closed: " + socket.getRemoteSocketAddress());
            } catch (IOException e) {
                AppLogger.error("Error closing socket", e);
            }
        }
    }
}
