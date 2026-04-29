package com.app.server;

import com.app.server.service.DbService;
import com.app.server.utils.AppLogger;
import com.app.server.network.Server;
import com.app.server.utils.TestPing;

/**
 * Server entry point.
 */
public class Main {
    public static void main(String[] ignoredArgs) {
        int port = Config.SERVER_PORT;
        AppLogger.info("Starting server on port " + port);

        DbService.initializeDatabase();

        new Thread(() -> new Server(port).start(), "Server-Thread").start();

        try {
            Thread.sleep(500);
        }
        catch (InterruptedException e) {
            AppLogger.error("Main thread interrupted while waiting for server to start", e.getMessage());
        }

        if (TestPing.testPing("127.0.0.1", port)) {
            AppLogger.info("PING test completed successfully.");
        }
        
    }

    

}