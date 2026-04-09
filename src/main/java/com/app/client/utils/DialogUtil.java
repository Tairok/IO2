package com.app.client.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;

import java.util.Optional;

public class DialogUtil {
    public static Optional<String> input(String title, String defaultVal) {
        TextInputDialog dialog = new TextInputDialog(defaultVal);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        return dialog.showAndWait();
    }



    public static void showError(String message, Exception e) {
        new Alert(Alert.AlertType.ERROR, message + ":\n" + e).showAndWait();
    }

}

