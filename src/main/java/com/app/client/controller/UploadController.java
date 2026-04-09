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
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UploadController {
    @FXML private Button chooseButton;
    @FXML private Button uploadButton;
    @FXML private VBox   filesContainer;
    @FXML private VBox   progressContainer;

    private String        username;
    private List<File>    filesToUpload;
    private FileService   fileService;
    // ExecutorService runs file transfer tasks in background threads to keep UI responsive
private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Must be called after FXML loading to
     * initialize username and connections.
     */
    public void init(String username, String serverHost, int serverPort) throws IOException {
        this.username = username;

        // 1) connection for commands
        NetworkConnection cmdConn = new NetworkConnection();
        cmdConn.open();
        CommandService cmdSvc = new CommandService(cmdConn);

        // 2) connection for file transfer
        NetworkConnection dataConn = new NetworkConnection();
        dataConn.open();
        TransferService txSvc = new TransferService(dataConn);

        // 3) FileService - facade for all file operations
        this.fileService = new FileService(cmdSvc, txSvc);

        // Initially button disabled until files are selected
        uploadButton.setDisable(true);
    }

    @FXML
    private void onChooseFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select files to upload");
        filesToUpload = chooser.showOpenMultipleDialog(stage());

        boolean any = filesToUpload != null && !filesToUpload.isEmpty();
        uploadButton.setDisable(!any);

        filesContainer.getChildren().setAll(
                any
                        ? filesToUpload.stream().map(f -> new Label(f.getName())).toList()
                        : List.<Label>of()
        );
    }

    @FXML
    private void onStartUpload() {
        progressContainer.getChildren().clear();

        for (File file : filesToUpload) {
            HBox row = new HBox(8);
            Label name = new Label(file.getName());
            ProgressBar pb = new ProgressBar(0);
            row.getChildren().addAll(name, pb);
            progressContainer.getChildren().add(row);

            // We create a task that uses FileService (with command+transfer)
            TransferTask task = new TransferTask(username, file, fileService);

            // Bind progress bar to progress in TransferTask
            pb.progressProperty().bind(task.progressProperty());

            task.setOnSucceeded(e -> name.setStyle("-fx-text-fill: green;"));
            task.setOnFailed(e -> name.setStyle("-fx-text-fill: red;"));

            executor.submit(task);
        }

        // after clicking "Start" we disable the button so as not to click twice
        uploadButton.setDisable(true);
    }

    private Stage stage() {
        return (Stage) chooseButton.getScene().getWindow();
    }
}
