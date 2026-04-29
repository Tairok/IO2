package com.app.server.controller;

import com.app.server.service.FileService;
import com.app.server.service.UserService;
import com.app.server.service.QueryService;
import com.app.server.service.UserFileShareService;
import com.app.server.utils.AppLogger;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;

public class ServerController {
    private final FileService            fileService     = new FileService();
    private final UserService            userService     = new UserService();
    private final QueryService           queryService    = new QueryService();
    private final UserFileShareService   shareService    = new UserFileShareService();

    /**
     * Handling one client: reads commands and delegates to appropriate services.
     */
    public void handleClient(Socket sock) {
        AppLogger.info("Client connected: " + sock.getInetAddress());
        try (DataInputStream dis = new DataInputStream(sock.getInputStream());
             DataOutputStream dos = new DataOutputStream(sock.getOutputStream())) {

            boolean running = true;
            while (running) {
                String cmd;
                try {
                    cmd = dis.readUTF();
                } catch (EOFException eof) {
                    AppLogger.info("Client closed connection");
                    break;
                }
                AppLogger.info("Received: " + cmd);

                switch (cmd) {
                    case "PING":
                        dos.writeUTF("PONG");
                        break;

                    case "LIST":
                        fileService.list(dis, dos);
                        break;

                    case "UPLOAD":
                        fileService.upload(dis, dos);
                        break;

                    case "DOWNLOAD":
                        fileService.download(dis, dos);
                        break;

                    case "RENAME":
                        break;

                    case "DELETE":
                        fileService.delete(dis, dos);
                        break;

                    case "CREATE":
                        fileService.create(dis, dos);
                        break;

                    case "QUERY":
                        queryService.query(dis, dos);
                        break;

                    case "EXECUTE":
                        queryService.executeUpdate(dis, dos);
                        break;

                    case "LOGIN":
                        userService.login(dis, dos);
                        break;

                    case "REGISTER":
                        userService.register(dis, dos);
                        break;

                    case "CHECK_USER":
                        userService.checkUser(dis, dos);
                        break;

                    case "CHECK_EMAIL":
                        userService.checkEmail(dis, dos);
                        break;
                    case "SHARE":
                        shareService.share(dis, dos);
                        break;

                    case "CHECK_PASSWORD":
                        userService.checkPassword(dis, dos);
                        break;
                    case "QUIT":
                        running = false;
                        break;
                    default:
                        dos.writeUTF("ERR\tUNKNOWN_COMMAND");
                }
                dos.flush();
            }

        } catch (IOException e) {
            AppLogger.error("Error in client handler", e
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                sock.close();
            } catch (IOException e) {
                AppLogger.error("Error closing client socket", e);
            }
            AppLogger.info("Client disconnected: " + sock.getInetAddress());
        }
    }
}
