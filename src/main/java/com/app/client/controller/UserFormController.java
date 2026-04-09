package com.app.client.controller;

import com.app.client.model.User;
import com.app.client.service.UserService;
import com.app.client.utils.Security;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class UserFormController {

    @FXML private TextField idField;
    @FXML private TextField loginField;
    @FXML private PasswordField passwordField;
    @FXML private TextField emailField;
    @FXML private TextField fullNameField;
    @FXML private ComboBox<String> roleCombo;

    private User user;
    private UserService userService;
    private boolean editMode;

    @FXML
    public void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList("user", "admin"));
        roleCombo.getSelectionModel().selectFirst();
    }

    public void setUser(User user, UserService userService) {
        this.user = user;
        this.userService = userService;
        this.editMode = user != null && user.getId() > 0;

        if (editMode) {
            idField.setText(String.valueOf(user.getId()));
        } else {
            idField.clear();
        }
        loginField.setText(user.getLogin() != null ? user.getLogin() : "");
        emailField.setText(user.getEmail() != null ? user.getEmail() : "");
        fullNameField.setText(user.getFullName() != null ? user.getFullName() : "");
        passwordField.clear();

        String role = user.getRole();
        if (role != null && roleCombo.getItems().contains(role)) {
            roleCombo.getSelectionModel().select(role);
        } else {
            roleCombo.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void onSave() {
        String login = loginField.getText().trim();
        String fullName = fullNameField.getText().trim();

        if (login.isEmpty() || fullName.isEmpty()) {
            new Alert(Alert.AlertType.WARNING,
                    "Login and name cannot be empty").showAndWait();
            return;
        }

        if (user == null || userService == null) {
            new Alert(Alert.AlertType.ERROR, "Form is not initialized properly.").showAndWait();
            return;
        }

        user.setLogin(login);
        user.setEmail(emailField.getText().trim());
        user.setFullName(fullName);
        user.setRole(roleCombo.getValue());

        String passwordInput = passwordField.getText();
        boolean isNew = user.getId() <= 0;

        if (isNew && (passwordInput == null || passwordInput.isBlank())) {
            new Alert(Alert.AlertType.WARNING, "Password is required when creating a user.").showAndWait();
            return;
        }

        if (passwordInput != null && !passwordInput.isBlank()) {
            user.setPassword(Security.hashPassword(passwordInput));
        }

        try {
            if (isNew) {
                userService.create(user);
            } else {
                userService.update(user);
            }
            ((Stage) idField.getScene().getWindow()).close();
        } catch (IOException | IllegalArgumentException e) {
            new Alert(Alert.AlertType.ERROR, "Saving user failed: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onCancel() {
        ((Stage) idField.getScene().getWindow()).close();
    }
}
