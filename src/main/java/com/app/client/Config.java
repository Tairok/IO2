package com.app.client;

import java.util.regex.Pattern;

public class Config {
    // Path constants for FXML files
    public static final String FXML_PATH = "/fxml/";
    public static final String FXML_ADMIN_PATH = "/fxml/admin/";

    // Server connection details
    public static final String SERVER_HOST = "127.0.0.1";
    public static final int SERVER_PORT = 5555;

    // Login validation pattern: 3-20 characters, letters, digits, period, underscore, minus
    public static final Pattern LOGIN_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{3,20}$");

    // Minimum password length: at least 6 characters, can be expanded
    public static final int MIN_PASSWORD_LENGTH = 1;

    // Private constructor to prevent instantiation of utility class
    private Config() {}
}
