package com.app.client.controller;

import com.app.client.utils.Tools;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Admin panel controller.
 */
public class AdminPanelController {

    @FXML private Label welcomeLabel;
    @FXML private TabPane tabPane;

    public void setAdminName(String name) {
        welcomeLabel.setText("Witaj, " + name + "!");
    }

    /**
     * Logs out and returns to the login screen.
     */
    @FXML
    private void handleLogout() throws IOException {
        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        stage.close();

        FXMLLoader loader = Tools.loadFXML("login");
        Parent root = loader.load();
        stage.setScene(new Scene(root));
        stage.setTitle("Rejestracja");
        stage.show();

    }
}
