package com.app.server;

import com.app.server.service.DbService;
import com.app.server.utils.AppLogger;
import com.app.server.network.Server;
import com.app.server.utils.TestPing;

public class Main {
    public static void main(String[] ignoredArgs) {
        int port = Config.SERVER_PORT;
        AppLogger.info("Starting server on port " + port);

        // Initialize database with schema and data
        DbService.initializeDatabase();

        // 1) Start the server in the background
        new Thread(() -> new Server(port).start(), "Server-Thread").start();

        // 2) Wait for the listener to start
        try {
            Thread.sleep(500);
        } 
        catch (InterruptedException e) {
            AppLogger.error("Main thread interrupted while waiting for server to start", e.getMessage());
        }

        // 3) Test PING → PONG
        if (TestPing.testPing("127.0.0.1", port))
            AppLogger.info("PING test completed successfully.");
        
    }

    

}