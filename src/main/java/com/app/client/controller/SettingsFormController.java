package com.app.client.controller;

import com.app.client.model.Setting;
import com.app.client.service.SettingsService;
import com.app.client.service.CommandService;
import com.app.client.utils.AppLogger;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

import static com.app.client.utils.DialogUtil.showError;

public class SettingsFormController {

    @FXML private TextField userIdField;
    @FXML private TextField displayNameField;
    @FXML private TextField bgColorField;

    private Setting setting;          // null → tworzymy nowe
    private SettingsService service;

    /** Musi być wywołane przed otwarciem formularza */
    public void setServices(CommandService cmdSvc) {
        this.service = new SettingsService(cmdSvc);
    }

    @FXML
    public void initialize() {
        // nic do inicjalizacji
    }

    /** Wywołuj po loader.getController(), ale PRZED stage.show() */
    public void initData(Setting s) {
        this.setting = s;

        if (s != null) {
            userIdField.setText(String.valueOf(s.getUserId()));
            displayNameField.setText(s.getDisplayName());
            bgColorField.setText(s.getBackgroundColor());
        }
    }

    @FXML
    private void onSave() {
        try {
            int userId = Integer.parseInt(userIdField.getText().trim());

            if (setting == null) {
                Setting newSetting = new Setting(
                        userId,
                        displayNameField.getText(),
                        bgColorField.getText()
                );
                service.create(newSetting);
            } else {
                setting.setUserId(userId);
                setting.setDisplayName(displayNameField.getText());
                setting.setBackgroundColor(bgColorField.getText());
                service.update(setting);
            }

            ((Stage) userIdField.getScene().getWindow()).close();

        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "Invalid number format").showAndWait();
        } catch (IOException e) {
            // skrócony komunikat dla użytkownika
            showError("Error saving setting", e);

            // pełny log do loggera
            AppLogger.error("Error saving setting", e);
        }
    }

    @FXML
    private void onCancel() {
        ((Stage) userIdField.getScene().getWindow()).close();
    }
}