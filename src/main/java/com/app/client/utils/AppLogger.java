package com.app.client.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppLogger {

    private static Logger getLogger() {
        String className = Thread.currentThread().getStackTrace()[3].getClassName();
        return LoggerFactory.getLogger(className);
    }

    public static void info(String message) {
        getLogger().info(message);
    }

    public static void error(String message, Throwable t) {
        getLogger().error(message, t);
    }
    public static void error(String message, String t) {
        getLogger().error(message, t);
    }

    public static void warn(String message) {
        getLogger().warn(message);
    }

    public static void debug(String message) {
        getLogger().debug(message);
    }
}
