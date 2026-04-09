package com.app.client.controller;

import com.app.client.model.User;
import com.app.client.service.UserService;
import com.app.client.network.Client;
import com.app.client.service.CommandService;
import com.app.client.utils.Tools;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

import static com.app.client.utils.DialogUtil.showError;

public class UserController {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colUId;
    @FXML private TableColumn<User, String>  colULogin, colUFullName, colUEmail, colURole;

    private CommandService cmdService;
    private UserService    service;

    @FXML
    public void initialize() {

        colUId       .setCellValueFactory(d -> d.getValue().idProperty().asObject());
        colULogin    .setCellValueFactory(d -> d.getValue().loginProperty());
        colUFullName .setCellValueFactory(d -> d.getValue().fullNameProperty());
        colUEmail    .setCellValueFactory(d -> d.getValue().emailProperty());
        colURole     .setCellValueFactory(d -> d.getValue().roleProperty());

        // 2) connection and service
        try {
            var client     = new Client();
            client.connect();
            cmdService = client.getService();
            service    = new UserService(cmdService);
        } catch (IOException e) {
            showError("Failed to connect to server", e);
            return;
        }


        loadData();
    }

    private void loadData() {
        try {
            userTable.setItems(FXCollections.observableArrayList(service.list()));
        } catch (IOException e) {
            showError("Error loading users", e);
        }
    }

    @FXML
    private void onAdd() {
        User newUser = new User();
        openForm(newUser, "New User");
    }

    @FXML
    private void onEdit() {
        User sel = userTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        // we copy only fields that exist in the new entity
        User copy = new User(
                sel.getId(),
                sel.getLogin(),
                sel.getPassword(),
                sel.getEmail(),
                sel.getFullName(),
                sel.getRole(),
                sel.getCreatedAt()
                
        );
        openForm(copy, "Edit User");
    }

    @FXML
    private void onDelete() {
        User sel = userTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        Optional<ButtonType> ok = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete user " + sel.getFullName() + "?").showAndWait();
        if (ok.isPresent() && ok.get() == ButtonType.OK) {
            try {
                service.delete(sel.getId());
                loadData();
            } catch (IOException e) {
                showError("Error deleting user", e);
            }
        }
    }

    private void openForm(User u, String title) {
        try {
            FXMLLoader loader = Tools.loadFXML("user_form");
            Parent root = loader.load();

            loader.getController();

            UserFormController ctrl = loader.getController();
            ctrl.setUser(u, service);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadData();
        } catch (Exception e) {
            showError("Error opening form", e);
        }
    }
}
