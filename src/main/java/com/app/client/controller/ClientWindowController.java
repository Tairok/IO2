package com.app.client.controller;

import com.app.client.model.FileEntry;
import com.app.client.network.NetworkConnection;
import com.app.client.service.CommandService;
import com.app.client.service.FileService;
import com.app.client.service.ShareService;
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

    @FXML private TextField shareRecipientField;
    @FXML private Button shareButton;
    @FXML private Button removeButton;
    @FXML private Button refreshButton;

    private String         currentUser;
    private FileService    fileService;
    private ShareService   shareService;
    private TransferService txService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

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
            this.shareService = new ShareService(commandService);

            onRefresh();

        } catch (IOException e) {
            showError("Connection Failed", e.getMessage());
            AppLogger.error("Connection Failed", e);
        }
    }

    @FXML
    private void onRefresh() {
        AppLogger.debug("onRefresh() called for user=" + currentUser);
        long start = System.currentTimeMillis();

        Task<List<FileEntry>> listTask = new Task<>() {
            @Override
            protected List<FileEntry> call() throws Exception {
                AppLogger.debug("Fetching file list from server...");
                return fileService.listFiles(currentUser);
            }
        };

        listTask.setOnSucceeded(e -> {
            List<FileEntry> files = listTask.getValue();
            AppLogger.debug("File list received: " + files.size() + " entries");
            fileTable.setItems(FXCollections.observableArrayList(files));
            long time = System.currentTimeMillis() - start;
            AppLogger.debug("onRefresh() completed in " + time + " ms");
        });

        listTask.setOnFailed(e -> {
            Throwable ex = listTask.getException();
            AppLogger.error("onRefresh() FAILED: " + ex.getMessage(), ex);
            showError("Refresh failed", ex.getMessage());
        });

        cmdExecutor.submit(listTask);
    }

    @FXML
    private void onDelete() {
        List<FileEntry> selectedFiles = new ArrayList<>(fileTable.getSelectionModel().getSelectedItems());

        if (selectedFiles.isEmpty()) {
            AppLogger.warn("onDelete() called but no file selected");
            new Alert(Alert.AlertType.WARNING, "Select at least one file").showAndWait();
            return;
        }

        AppLogger.debug("onDelete() called. Selected files: " + selectedFiles.size());

        cmdExecutor.submit(() -> {
            long start = System.currentTimeMillis();
            try {
                for (FileEntry fe : selectedFiles) {
                    AppLogger.debug("Deleting file: " + fe.getFilename());
                    boolean ok = fileService.deleteFile(currentUser, fe.getFilename());
                    if (!ok) {
                        AppLogger.warn("Server returned false for delete: " + fe.getFilename());
                    } else {
                        AppLogger.debug("File deleted successfully: " + fe.getFilename());
                    }
                }
                long time = System.currentTimeMillis() - start;
                AppLogger.debug("Delete operation completed in " + time + " ms");
                Platform.runLater(this::onRefresh);

            } catch (Exception e) {
                AppLogger.error("Delete failed: " + e.getMessage(), e);
                Platform.runLater(() -> showError("Delete failed", e.getMessage()));
            }
        });
    }

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
                onRefresh();
                closeQuietly(fileConn);
            });
            task.setOnFailed(e -> {
                Throwable ex = task.getException();
                if ("Quota exceeded".equals(String.valueOf(e))) {
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
                name.setStyle(ok ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
            });
            task.setOnFailed(e -> {
                name.setStyle("-fx-text-fill: red;");
                AppLogger.error("Download failed", task.getException());
            });

            executor.submit(task);
        }
    }

    @FXML
    private void onShare(ActionEvent ev) {
        String recipient = shareRecipientField.getText();
        List<FileEntry> selected = new ArrayList<>(fileTable.getSelectionModel().getSelectedItems());
        List<String> filenames = new ArrayList<>();

        for (FileEntry fe : selected) {
            filenames.add(fe.getFilename());
        }

        cmdExecutor.submit(() -> {
            try {
                // Korzystamy z nowej metody shareFiles z ShareService (zaimplementowanej w Commicie 3)
                List<String> sharedFiles = shareService.shareFiles(currentUser, recipient, filenames);

                Platform.runLater(() -> {
                    new Alert(Alert.AlertType.INFORMATION,
                            "Shared " + sharedFiles.size() + " file(s) with " + recipient).showAndWait();
                    shareRecipientField.clear();
                });
            } catch (IllegalArgumentException e) {
                // Przechwytywanie wyjątków walidacji z ShareService
                Platform.runLater(() ->
                        new Alert(Alert.AlertType.WARNING, e.getMessage()).showAndWait()
                );
            } catch (Exception e) {
                AppLogger.error("Sharing failed", e);
                Platform.runLater(() ->
                        new Alert(Alert.AlertType.ERROR,
                                "Cannot share files:\n" + e.getMessage()).showAndWait()
                );
            }
        });
    }

    public void onLogout() {
        transferExecutor.shutdownNow();
        cmdExecutor.shutdownNow();

        Stage oldStage = getStage();
        oldStage.close();

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
}