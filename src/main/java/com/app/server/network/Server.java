package com.app.server.network;

import com.app.server.controller.ServerController;
import com.app.server.utils.AppLogger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final int port;
    private final ExecutorService pool      = Executors.newCachedThreadPool();
    private final ServerController controller = new ServerController();

    public Server(int port) {
        this.port = port;
    }

    /**
     * Starts the listening loop on port `port`.
     * Each connection is passed to ServerController.
     */
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            AppLogger.info("Server listening on port " + port);
            while (true) {
                Socket client = serverSocket.accept();
                pool.submit(() -> controller.handleClient(client));
            }
        } catch (IOException e) {
            AppLogger.error("Server error", e);
        }
    }
}
