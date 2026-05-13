package com.app.client.utils;

import javafx.fxml.FXMLLoader;

import static com.app.client.Config.*;

/**
 * Client-side utilities.
 */
public class Tools {

    /**
     * Loads an FXML resource using the configured {@code FXML_PATH}.
     *
     * @param filename FXML filename without extension
     * @return configured loader
     */
    public static FXMLLoader loadFXML(String filename) {
        return new FXMLLoader(Tools.class.getResource(FXML_PATH + filename + ".fxml"));
    }
}
