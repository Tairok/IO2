package com.app.client;

import com.app.client.utils.AppLogger;
import com.app.client.utils.Tools;
import com.app.client.utils.TestPing;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends Application {

    private static final Logger log = LoggerFactory.getLogger(Main.class);
    int port = Config.SERVER_PORT;
    @Override
    public void start(@SuppressWarnings("exports") Stage primaryStage) {
        log.info("Starting Cloud Storage Client...");

        if (TestPing.testPing("127.0.0.1", port))
            AppLogger.info("PING test completed successfully.");

        try {
            FXMLLoader loader = Tools.loadFXML("login");
            Parent root = loader.load();

            primaryStage.setTitle("Login");
            Scene sc = new Scene(root, 350, 350);
            sc.getStylesheets().add(
                    Objects.requireNonNull(
                    getClass().getResource("/fxml/css/style.css")
                ).toExternalForm()
            );
            primaryStage. setScene(sc);
            primaryStage.show();

            log.info("Login interface loaded successfully.");
        } catch (Exception e) {
            log.error("Failed to load login UI.", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
