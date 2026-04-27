package com.app.client.utils;

import javafx.scene.control.Alert;

public class DialogUtil {


    public static void showError(String message, Exception e) {
        new Alert(Alert.AlertType.ERROR, message + ":\n" + e).showAndWait();
    }

}

