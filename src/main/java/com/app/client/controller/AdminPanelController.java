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

public class AdminPanelController {

    @FXML private Label welcomeLabel;
    @FXML private TabPane tabPane;      // ← nowa referencja do TabPane

    public void setAdminName(String name) {
        welcomeLabel.setText("Witaj, " + name + "!");
    }

    @FXML
    private void handleLogout() throws IOException {
        // Zamknij panel
        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        stage.close();

        //OPEN LOGIN
        FXMLLoader loader = Tools.loadFXML("login");
        Parent root = loader.load();
        stage.setScene(new Scene(root, 300, 275));
        stage.setTitle("Rejestracja");
        stage.show();

    }
}
