package com.app.client.controller;

import com.app.client.network.NetworkConnection;
import com.app.client.service.CommandService;
import com.app.client.service.FileService;
import com.app.client.service.TransferService;
import com.app.client.task.TransferTask;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Transfer controller used for initiating uploads.
 */
public class TransferController {
    @FXML private Button startButton;
    @FXML private VBox   filesContainer;

    private String            username;
    private List<File>        filesToUpload;
    private NetworkConnection conn;

    private TransferService   transferService;

    private ExecutorService executor;

    @FXML
    public void initialize() {
        executor = Executors.newCachedThreadPool();
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
        this.transferService = new TransferService(conn);
    }
}
