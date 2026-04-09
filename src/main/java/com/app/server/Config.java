package com.app.server;

public class Config {
    // Database connection details for MariaDB
    public static final String DB_URL = "jdbc:mariadb://localhost:3306/io";
    public static final String DB_USERNAME = "root";
    public static final String DB_PASSWORD = "zaq1@WSX";

    // TCP server port
    public static final int SERVER_PORT = 5555;

    // Directory where received files will be saved
    public static final String RECEIVED_FILES_PATH = "./files/";
    /** Number of bytes in 1 GB */
    public static final long BYTES_PER_GB = 1024L * 1024 * 1024;

    // Private constructor for utility class - no instances created
    private Config() {
        // Utility class - do not create instances
    }
}
