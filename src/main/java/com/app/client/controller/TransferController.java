package com.app.client.controller;

import com.app.client.network.NetworkConnection;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.List;

/**
 * Transfer controller used for initiating uploads.
 */
public class TransferController {
    @FXML private Button startButton;
    @FXML private VBox   filesContainer;

    private String            username;
    private List<File>        filesToUpload;
    private NetworkConnection conn;

    @FXML
    public void initialize() {
    }

    /**
     * Call immediately after FXML load to give controller its context.
     */
    public void init(String username,
                     List<File> filesToUpload,
                     NetworkConnection conn) {
        this.username      = username;
        this.filesToUpload = filesToUpload;
        this.conn          = conn;
    }
}
