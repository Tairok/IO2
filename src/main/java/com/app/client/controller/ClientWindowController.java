package com.app.client.controller;

import com.app.client.model.FileEntry;
import com.app.client.network.NetworkConnection;
import com.app.client.service.CommandService;
import com.app.client.service.FileService;
import com.app.client.service.TransferService;
import com.app.client.task.TransferTask;
import com.app.client.utils.AppLogger;
import com.app.client.utils.Tools;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientWindowController {
    @FXML private Label welcomeLabel;
    @FXML private TableView<FileEntry> fileTable;
    @FXML private TableColumn<FileEntry,String> filenameColumn;
    @FXML private TableColumn<FileEntry,Long>   sizeColumn;
    @FXML private TableColumn<FileEntry,String> lastModifiedColumn;
    @FXML private VBox progressContainer;
    // Removed shareRecipientField and removeButton
    



    private String         currentUser;
    private FileService    fileService;
    // Removed ShareService reference
    private TransferService txService;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    // Executor for file–transfer tasks (uploads/downloads):
    // transferExecutor runs file upload/download tasks in background threads to keep UI responsive
private final ExecutorService transferExecutor = Executors.newCachedThreadPool();

    // SINGLE-THREAD executor for all control commands, so they never overlap:
    private final ExecutorService cmdExecutor = Executors.newSingleThreadExecutor();

    @FXML
    public void initialize() {
        filenameColumn.setCellValueFactory(c -> c.getValue().filenameProperty());
        sizeColumn    .setCellValueFactory(c -> c.getValue().sizeProperty().asObject());
        lastModifiedColumn.setCellValueFactory(c -> c.getValue().lastModifiedProperty());

        // enable multi-select
        fileTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    /** Called by your LoginController after successful login **/
    public void setUsername(String user) {
       this.currentUser = user;
       welcomeLabel.setText("Welcome, " + user);
        try {
            NetworkConnection conn = new NetworkConnection();
            conn.open();
            CommandService commandService = new CommandService(conn);
            this.txService = new TransferService(conn);
            this.fileService = new FileService(commandService, txService);
                // Removed ShareService initialization
                // Removed onRefresh call
        } catch (IOException e) {
            showError("Connection Failed", e.getMessage());
            AppLogger.error("Connection Failed", e);
        }
    }

    // Removed onRefresh method



    // Removed onDelete method

    @FXML
    private void onUpload() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select files to upload");
        List<File> files = fc.showOpenMultipleDialog(getStage());
        if (files == null || files.isEmpty()) return;

        progressContainer.getChildren().clear();

        for (File f : files) {
            HBox row = new HBox(5);
            Label name = new Label(f.getName());
            ProgressBar pb = new ProgressBar(0);
            row.getChildren().addAll(name, pb);
            progressContainer.getChildren().add(row);

            // NEW: spin up a fresh connection + service per file
            NetworkConnection fileConn = new NetworkConnection();
            try {
                fileConn.open();
            } catch (IOException e) {
                name.setStyle("-fx-text-fill: red;");
                AppLogger.error("Cannot open connection for " + f.getName(), e);
                continue;
            }
            CommandService   fileCmd  = new CommandService(fileConn);
            TransferService  fileTx   = new TransferService(fileConn);
            FileService      fileSvc  = new FileService(fileCmd, fileTx);

            TransferTask task = new TransferTask(currentUser, f, fileSvc);
            pb.progressProperty().bind(task.progressProperty());

            task.setOnSucceeded(e -> {
                name.setStyle("-fx-text-fill: green;");
                closeQuietly(fileConn);
            });
            task.setOnFailed(e -> {
                Throwable ex = task.getException();
                if ("Quota exceeded".equals(e)) {
                    Platform.runLater(() ->
                            new Alert(Alert.AlertType.ERROR,
                                    "Not uploaded – exceeded space limit for your plan.")
                                    .showAndWait()
                    );
                }
                name.setStyle("-fx-text-fill: red;");
                AppLogger.error("Upload failed for " + f.getName(), String.valueOf(e));
                closeQuietly(fileConn);
            });

            transferExecutor.submit(task);
        }
    }

    private void closeQuietly(NetworkConnection conn) {
        try { conn.close(); } catch (IOException ignored) {}
    }


    @FXML
    private void onDownload() {
        List<FileEntry> selected = fileTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Select at least one file").showAndWait();
            return;
        }

        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Choose download folder");
        File destDir = dc.showDialog(getStage());
        if (destDir == null) return;

        progressContainer.getChildren().clear();
        for (FileEntry fe : selected) {
            HBox row = new HBox(5);
            Label name = new Label(fe.getFilename());
            ProgressBar pb = new ProgressBar(0);
            row.getChildren().addAll(name, pb);
            progressContainer.getChildren().add(row);

            Task<Boolean> task = new Task<>() {
                @Override protected Boolean call() throws Exception {
                    File out = new File(destDir, fe.getFilename());
                    return fileService.downloadFile(
                            currentUser,
                            fe.getFilename(),
                            out,
                            (rec, tot) -> updateProgress(rec, tot)
                    );
                }
            };
            pb.progressProperty().bind(task.progressProperty());

            task.setOnSucceeded(e -> {
                boolean ok = task.getValue();
                name.setStyle(ok
                        ? "-fx-text-fill: green;"
                        : "-fx-text-fill: red;");
            });
            task.setOnFailed(e -> {
                name.setStyle("-fx-text-fill: red;");
                AppLogger.error("Download failed", task.getException());
            });

            executor.submit(task);
        }
    }


        public void onLogout() {
            // Zatrzymaj wątki
            transferExecutor.shutdownNow();
            cmdExecutor.shutdownNow();

            // Zamknij obecne okno
            Stage oldStage = getStage();
            oldStage.close();

            // Otwórz login.fxml
            try {
                FXMLLoader loader = Tools.loadFXML("login");
                Parent root = loader.load();
                Stage loginStage = new Stage();
                loginStage.setTitle("Logowanie");
                loginStage.setScene(new Scene(root));
                loginStage.show();
            } catch (IOException e) {
                e.printStackTrace();

            }
        }


    private Stage getStage() {
        return (Stage) welcomeLabel.getScene().getWindow();
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setTitle(title);
        a.showAndWait();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "i";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }




    // Removed onShare method
}
