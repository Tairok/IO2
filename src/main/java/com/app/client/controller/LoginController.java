package com.app.client.controller;

import com.app.client.network.Client;
import com.app.client.service.CommandService;
import com.app.client.utils.AppLogger;
import com.app.client.utils.Tools;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private TextField    usernameField;
    @FXML private PasswordField passwordField;

    // novelty: network client and command service
    private final Client client = new Client();
    private CommandService svc;

    @FXML
    public void initialize() {
        try {
            client.connect();
            svc = client.getService();
            AppLogger.info("Connected to server");
        } catch (IOException e) {
            AppLogger.error("Failed to connect to server", e);
            showAlert("Error", "No connection to server: " + e, Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void handleLoginButtonAction() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        AppLogger.info("Attempting login for username: " + username);

        try {
            String role = svc.login(username, password);
            if (role != null) {
                AppLogger.info("Login successful for user: " + username + " with role: " + role);
                showAlert("Success", "Logged in successfully as " + role, Alert.AlertType.INFORMATION);
                closeWindow();

                if ("ADMIN".equalsIgnoreCase(role)) {
                    showAdminPanel(username);
                } else {
                    showClientWindow(username);
                }
            } else {
                AppLogger.warn("Login failed - Invalid credentials for user: " + username);
                showAlert("Login Error", "Invalid username or password", Alert.AlertType.ERROR);
            }
        } catch (IOException e) {
            AppLogger.error("Exception during login for user: " + username, e);
            showAlert("Error", "An error occurred during login: " + e, Alert.AlertType.ERROR);
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.close();
    }

    private void showAdminPanel(String username) {
        try {
            FXMLLoader loader = Tools.loadFXML("admin_panel");
            Parent root = loader.load();
            var controller = loader.getController();
            ((AdminPanelController)controller)
                    .setAdminName(username);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Admin Panel");
            stage.show();
        } catch (IOException e) {
            AppLogger.error("Error loading admin panel: ", e);
            showAlert("Error", "Problem opening admin panel: " + e, Alert.AlertType.ERROR);
        }
    }

    private void showClientWindow(String username) {
        try {
            FXMLLoader loader = Tools.loadFXML("client_window");

            Parent root = loader.load();
            var controller = loader.getController();
            ((com.app.client.controller.ClientWindowController)controller)
                    .setUsername(username);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("User Panel");
            stage.show();
        } catch (IOException e) {
            AppLogger.error("Error loading client window: ", e);
            showAlert("Error", "Problem opening user window: " + e, Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
