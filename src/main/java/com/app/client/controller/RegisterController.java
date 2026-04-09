package com.app.client.controller;

import com.app.client.model.Setting;
import com.app.client.network.Client;
import com.app.client.service.CommandService;
import com.app.client.service.SettingsService;
import com.app.client.service.UserService;
import com.app.client.utils.AppLogger;
import com.app.client.utils.Security;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;

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
            client.connect();         // <-- calls networkConnection.open()
            svc    = client.getService();

            userService     = new UserService(svc);
            settingsService = new SettingsService(svc);
            AppLogger.info("Connected to server for registration");
        } catch (Exception e) {
            AppLogger.error("Connection failed", e);
            showAlert("Error", "Cannot connect to server:\n" + e, Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void register(ActionEvent ev) {
        String login    = loginField.getText().trim();
        String fullName = fullNameField.getText().trim();
        String pwd      = passwordField.getText();
        String pwd2     = confirmPasswordField.getText();
        String email    = emailField.getText().trim();

        // Prosta walidacja
        if (login.isEmpty() || fullName.isEmpty() || pwd.isEmpty() ||
                pwd2.isEmpty()  || email.isEmpty()) {
            showAlert("Validation", "All fields must be filled.", Alert.AlertType.WARNING);
            return;
        }
        if (!pwd.equals(pwd2)) {
            showAlert("Validation", "Passwords do not match.", Alert.AlertType.WARNING);
            return;
        }

        try {
            // Check availability of login and email
            if (userService.isUserExists(login)) {
                showAlert("Error", "Login is already taken.", Alert.AlertType.ERROR);
                return;
            }
            if (userService.isEmailExists(email)) {
                showAlert("Error", "E-mail is already registered.", Alert.AlertType.ERROR);
                return;
            }

            // 1) Register user
            String hash = Security.hashPassword(pwd);
            String plan = "Starter"; // Default plan
            boolean registered = userService.register(login, fullName, hash, email, plan);
            if (!registered) {
                showAlert("Error", "Registration failed.", Alert.AlertType.ERROR);
                return;
            }

            // 2) Get new user ID
            int userId = userService.getUserIdByLogin(login);

            // 3) Save default user settings
            settingsService.create(new Setting(userId, fullName));

            // 4) Sukces
            showAlert("Success", "Registration complete!", Alert.AlertType.INFORMATION);
            ((Stage)loginField.getScene().getWindow()).close();

        } catch (Exception e) {
            AppLogger.error("Registration error", e);
            showAlert("Error", e.getMessage(), Alert.AlertType.ERROR);
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
