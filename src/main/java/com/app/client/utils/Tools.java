package com.app.client.utils;

import com.app.client.Config;
import com.app.client.network.Client;
import javafx.fxml.FXMLLoader;

import java.net.ConnectException;
import java.util.regex.Pattern;

import static com.app.client.Config.*;

public class Tools {
   

    public static boolean testPing() {
        Client client = new Client();
        boolean result;

        try {
            AppLogger.info("Testing connection to server...");
            client.connect();

            if (client.ping()) {
                AppLogger.info("Received PONG server is up!");
                result = true;
            } else {
                AppLogger.warn("Server did not respond.");
                result = false;
            }
        }
        catch (ConnectException ce) {
            AppLogger.error("Cannot connect to server  make sure it is running and listening on " +
                    Config.SERVER_HOST + ":" + Config.SERVER_PORT, ce);
            result = false;
        }
        catch (Exception e) {
            AppLogger.error(" Error during PING test:", e);
            result = false;
        }
        finally {
            try {
                client.disconnect();
                AppLogger.info(" Connection closed.");
            }
            catch (Exception e) {
                // if disconnect also throws, we can log it but keep running
                AppLogger.warn(" Error while closing connection:");
            }
        }

        return result;
    }


    // Funkcja ładująca dowolne FXML
    public static FXMLLoader loadFXML(String filename) {
        return new FXMLLoader(Tools.class.getResource(FXML_PATH + filename + ".fxml"));
    }

    // Function loading FXML specific for admin panel
    /*
    public static FXMLLoader loadAdminFXML(String filename) {
        FXMLLoader loader = new FXMLLoader(Tools.class.getResource(FXML_ADMIN_PATH +filename +".fxml"));
        return loader;
    }*/

    // simple regex for email (local part + @ + domain)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    );





    /**
     * Sprawdza poprawność formatu adresu e-mail.
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Checks if login meets length and allowed characters conditions.
     */
    public static boolean isValidLogin(String login) {
        if (login == null) return false;
        return LOGIN_PATTERN.matcher(login).matches();
    }

    /**
     * Checks minimum password length.
     */
    public static boolean isValidPassword(String password) {
        if (password == null) return false;
        return password.length() >= MIN_PASSWORD_LENGTH;
    }


}
