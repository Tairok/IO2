package com.app.server;

/**
 * Application-wide server configuration.
 *
 * <p>Contains network and storage parameters used by the server runtime.
 */
public final class Config {

    /** JDBC URL for MariaDB connection. */
    public static final String DB_URL = "jdbc:mariadb://localhost:3306/io";

    /** Database username for server connection. */
    public static final String DB_USERNAME = "root";

    /** Database password for server connection. */
    public static final String DB_PASSWORD = "zaq1@WSX";

    /** TCP port for the server listener. */
    public static final int SERVER_PORT = 5555;

    /** Directory where received files are stored on the server. */
    public static final String RECEIVED_FILES_PATH = "./files/";

    /** Number of bytes in 1 GB. */
    public static final long BYTES_PER_GB = 1024L * 1024 * 1024;

    private Config() {
    }
}
