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

public class TransferController {
    @FXML private Button startButton;
    @FXML private VBox   filesContainer;

    // injected before showing this screen:
    private String            username;
    private List<File>        filesToUpload;
    private NetworkConnection conn;

    // we use the same TransferService (and conn) for all files
    private TransferService   transferService;

    // pool for parallel uploads
    // ExecutorService runs file transfer tasks in background threads to keep UI responsive
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
    /*
    @FXML
    private void onStartUpload() {
        startButton.setDisable(true);

        // create a CommandService once for control commands
        CommandService cmd = new CommandService(conn);
        FileService    svc = new FileService(cmd, transferService);

        for (File file : filesToUpload) {
            HBox row = new HBox(10);
            Label name = new Label(file.getName());
            ProgressBar pb = new ProgressBar(0);
            row.getChildren().addAll(name, pb);
            filesContainer.getChildren().add(row);

            // each task gets the same FileService
            TransferTask task = new TransferTask(username, file, svc);
            pb.progressProperty().bind(task.progressProperty());

            task.setOnSucceeded(evt ->
                    row.setStyle("-fx-background-color: lightgreen;")
            );
            task.setOnFailed(evt ->
                    row.setStyle("-fx-background-color: lightcoral;")
            );

            executor.submit(task);
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }*/
}
