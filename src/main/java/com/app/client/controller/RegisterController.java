package com.app.client.controller;

import com.app.client.network.Client;
import com.app.client.service.CommandService;
import com.app.client.service.SettingsService;
import com.app.client.service.UserService;
import com.app.client.utils.AppLogger;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

/**
 * Registration screen controller.
 */
public class RegisterController {
    @FXML private TextField        loginField;
    @FXML private TextField        fullNameField;
    @FXML private PasswordField    passwordField;
    @FXML private PasswordField    confirmPasswordField;
    @FXML private TextField        emailField;

    private Client          client;

    private UserService     userService;
    private SettingsService settingsService;
    private  CommandService svc;


    @FXML
    public void initialize() throws IOException {
        try {
            client = new Client();
            client.connect();
            svc    = client.getService();

            userService     = new UserService(svc);
            settingsService = new SettingsService(svc);
            AppLogger.info("Connected to server for registration");
        } catch (Exception e) {
            AppLogger.error("Connection failed", e);
            showAlert("Error", "Cannot connect to server:\n" + e, Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
