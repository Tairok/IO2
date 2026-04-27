package com.app.client.utils;

import javafx.fxml.FXMLLoader;

import java.util.regex.Pattern;

import static com.app.client.Config.*;

public class Tools {


    // Funkcja ładująca dowolne FXML
    public static FXMLLoader loadFXML(String filename) {
        return new FXMLLoader(Tools.class.getResource(FXML_PATH + filename + ".fxml"));
    }

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    );




}
