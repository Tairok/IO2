package com.app.client.controller;

import com.app.client.model.FileEntry;
import com.app.client.network.NetworkConnection;
import com.app.client.service.CommandService;
import com.app.client.service.FileService;
import com.app.client.service.TransferService;
import com.app.client.task.TransferTask;
import com.app.client.utils.AppLogger;

import com.app.client.utils.Tools;
import com.app.client.patterns.observers.TransferEvent;
import com.app.client.patterns.observers.TransferNotificationCenter;
import com.app.client.patterns.observers.TransferObserver;
import com.app.client.patterns.observers.TransferStage;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.util.Duration;
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


/**
 * Main client window controller (file list, uploads/downloads, sharing, user list).
 */
public class ClientWindowController {
    @FXML private Label welcomeLabel;
    @FXML private TableView<FileEntry> fileTable;
    @FXML private TableColumn<FileEntry,String> filenameColumn;
    @FXML private TableColumn<FileEntry,Long>   sizeColumn;
    @FXML private TableColumn<FileEntry,String> lastModifiedColumn;
    @FXML private VBox progressContainer;
    @FXML private javafx.scene.control.TabPane mainTabPane;
    @FXML private TextField shareRecipientField;

    @FXML private Button removeButton;

    @FXML private TableView<com.app.client.model.User> userTable;
    @FXML private TableColumn<com.app.client.model.User,String> userLoginColumn;
    @FXML private TableColumn<com.app.client.model.User,String> userRoleColumn;
    @FXML private TableColumn<com.app.client.model.User,String> userEmailColumn;

    private com.app.client.service.CommandService commandService;

    private String         currentUser;
    private FileService    fileService;
    private TransferService txService;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ExecutorService transferExecutor = Executors.newCachedThreadPool();

    private final ExecutorService cmdExecutor = Executors.newSingleThreadExecutor();

    private final Object confirmationLock = new Object();
    private final List<String> pendingConfirmations = new ArrayList<>();
    private PauseTransition confirmationFlush;

    private final TransferObserver confirmationObserver = this::handleTransferEvent;

    @FXML
    public void initialize() {
        filenameColumn.setCellValueFactory(c -> c.getValue().filenameProperty());
        sizeColumn    .setCellValueFactory(c -> c.getValue().sizeProperty().asObject());
        lastModifiedColumn.setCellValueFactory(c -> c.getValue().lastModifiedProperty());

        fileTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        userLoginColumn.setCellValueFactory(c -> c.getValue().loginProperty());
        userRoleColumn.setCellValueFactory(c -> c.getValue().roleProperty());
        userEmailColumn.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                "ADMIN".equalsIgnoreCase(c.getValue().getRole()) ? c.getValue().getEmail() : ""
        ));
        userTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        TransferNotificationCenter.getInstance().addObserver(confirmationObserver);
    }

    /** Called by your LoginController after successful login **/
    public void setUsername(String user) {
       this.currentUser = user;
       welcomeLabel.setText("Welcome, " + user);
        try {
            NetworkConnection conn = new NetworkConnection();
            conn.open();
            this.commandService = new CommandService(conn);
            this.txService = new TransferService(conn);
            this.fileService = new FileService(this.commandService, txService);
            onRefresh();
            loadUsers();
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

        loadUsers();

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

                Platform.runLater(() -> {
                    showError("Delete failed", e.getMessage());
                });
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
            VBox row = new VBox(3);
            HBox fileRow = new HBox(5);
            Label name = new Label(f.getName());
            ProgressBar pb = new ProgressBar(0);
            fileRow.getChildren().addAll(name, pb);
            
            Label progressLabel = new Label("0 / " + formatBytes(f.length()));
            progressLabel.setStyle("-fx-font-size: 11;");
            row.getChildren().addAll(fileRow, progressLabel);
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

            TransferTask task = new TransferTask(currentUser, f, fileSvc, (sent, total) ->
                    Platform.runLater(() ->
                            progressLabel.setText(formatBytes(sent) + " / " + formatBytes(total))
                    )
            );
            pb.progressProperty().bind(task.progressProperty());

            task.setOnSucceeded(e -> {
                name.setStyle("-fx-text-fill: green;");
                progressLabel.setText(formatBytes(f.length()) + " / " + formatBytes(f.length()));
                onRefresh();
                closeQuietly(fileConn);
            });
            task.setOnFailed(e -> {
                Throwable ex = task.getException();
                if ("Quota exceeded".equals(ex.getMessage())) {
                    Platform.runLater(() ->
                            new Alert(Alert.AlertType.ERROR,
                                    "Not uploaded – exceeded space limit for your plan.")
                                    .showAndWait()
                    );
                }
                name.setStyle("-fx-text-fill: red;");
                AppLogger.error("Upload failed for " + f.getName(), ex);
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
            VBox row = new VBox(3);
            HBox fileRow = new HBox(5);
            Label name = new Label(fe.getFilename());
            ProgressBar pb = new ProgressBar(0);
            fileRow.getChildren().addAll(name, pb);
            
            Label progressLabel = new Label("0 / 0 bytes");
            progressLabel.setStyle("-fx-font-size: 11;");
            row.getChildren().addAll(fileRow, progressLabel);
            progressContainer.getChildren().add(row);

            NetworkConnection downloadConn = new NetworkConnection();
            try {
                downloadConn.open();
            } catch (IOException e) {
                name.setStyle("-fx-text-fill: red;");
                AppLogger.error("Cannot open connection for " + fe.getFilename(), e);
                continue;
            }
            
            CommandService downloadCmd = new CommandService(downloadConn);
            TransferService downloadTx = new TransferService(downloadConn);
            FileService downloadFileSvc = new FileService(downloadCmd, downloadTx);

            Task<Boolean> task = new Task<>() {
                @Override protected Boolean call() throws Exception {
                    File out = new File(destDir, fe.getFilename());
                    return downloadFileSvc.downloadFile(
                            currentUser,
                            fe.getFilename(),
                            out,
                            (rec, tot) -> {
                                updateProgress(rec, tot);
                                Platform.runLater(() -> {
                                    progressLabel.setText(formatBytes(rec) + " / " + formatBytes(tot));
                                });
                            }
                    );
                }
            };
            pb.progressProperty().bind(task.progressProperty());

            task.setOnSucceeded(e -> {
                boolean ok = task.getValue();
                name.setStyle(ok
                        ? "-fx-text-fill: green;"
                        : "-fx-text-fill: red;");
                closeQuietly(downloadConn);
            });
            task.setOnFailed(e -> {
                name.setStyle("-fx-text-fill: red;");
                AppLogger.error("Download failed", task.getException());
                closeQuietly(downloadConn);
            });

            transferExecutor.submit(task);
        }
    }


        public void onLogout() {
            TransferNotificationCenter.getInstance().removeObserver(confirmationObserver);
            if (confirmationFlush != null) {
                confirmationFlush.stop();
                confirmationFlush = null;
            }

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

    private void handleTransferEvent(TransferEvent event) {
        if (event == null) return;
        if (event.stage() != TransferStage.CONFIRMED) return;

        String line = switch (event.action()) {
            case UPLOAD -> "Wysłano (upload): " + event.filename();
            case DOWNLOAD -> "Pobrano (download): " + event.filename();
            case SHARE -> "Udostępniono: " + event.filename()
                    + " → " + (event.recipient() == null ? "" : event.recipient());
        };

        synchronized (confirmationLock) {
            pendingConfirmations.add(line);
        }

        if (event.action() == com.app.client.patterns.observers.TransferAction.UPLOAD ||
                event.action() == com.app.client.patterns.observers.TransferAction.SHARE) {
            Platform.runLater(this::onRefresh);
        }

        Platform.runLater(this::scheduleConfirmationFlush);
    }

    /** Load users list from server and show in userTable */
    private void loadUsers() {
        if (this.commandService == null) return;

        Task<List<com.app.client.model.User>> t = new Task<>() {
            @Override protected List<com.app.client.model.User> call() throws Exception {
                var rows = commandService.query("SELECT login, role, email FROM users");
                List<com.app.client.model.User> users = new ArrayList<>();
                for (String[] r : rows) {
                    com.app.client.model.User u = new com.app.client.model.User();
                    u.setLogin(r.length > 0 ? r[0] : "");
                    u.setRole(r.length > 1 ? r[1] : "");
                    u.setEmail(r.length > 2 ? r[2] : "");
                    users.add(u);
                }
                return users;
            }
        };

        t.setOnSucceeded(e -> {
            var list = t.getValue();
            userTable.setItems(FXCollections.observableArrayList(list));
        });
        t.setOnFailed(e -> AppLogger.error("loadUsers failed", t.getException()));

        cmdExecutor.submit(t);
    }

    private void scheduleConfirmationFlush() {
        if (confirmationFlush == null) {
            confirmationFlush = new PauseTransition(Duration.millis(400));
            confirmationFlush.setOnFinished(e -> flushConfirmations());
        }
        confirmationFlush.playFromStart();
    }

    private void flushConfirmations() {
        final List<String> lines;
        synchronized (confirmationLock) {
            if (pendingConfirmations.isEmpty()) return;
            lines = new ArrayList<>(pendingConfirmations);
            pendingConfirmations.clear();
        }

        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Potwierdzenia transferu");
        a.setHeaderText("Serwer potwierdził zakończenie operacji");
        a.setContentText(String.join("\n", lines));
        a.show();
    }



    @FXML
    private void onShare(ActionEvent ev) {
        String recipient = shareRecipientField.getText();
        if (recipient == null || recipient.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Enter recipient username").showAndWait();
            return;
        }

        List<FileEntry> selected = new ArrayList<>(fileTable.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Select at least one file to share").showAndWait();
            return;
        }

        progressContainer.getChildren().clear();

        for (FileEntry fe : selected) {
            VBox row = new VBox(3);
            HBox fileRow = new HBox(5);
            Label name = new Label(fe.getFilename());
            Label status = new Label("Sharing...");
            status.setStyle("-fx-font-size: 11; -fx-text-fill: #0099ff;");
            fileRow.getChildren().addAll(name, status);
            row.getChildren().add(fileRow);
            progressContainer.getChildren().add(row);

            cmdExecutor.submit(() -> {
                try {
                    fileService.shareFile(currentUser, recipient.trim(), fe.getFilename());
                    Platform.runLater(() -> {
                        name.setStyle("-fx-text-fill: green;");
                        status.setText("✓ Shared with " + recipient);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        name.setStyle("-fx-text-fill: red;");
                        status.setText("✗ " + ex.getMessage());
                    });
                    AppLogger.error("Share failed", ex);
                }
            });
        }

        Platform.runLater(() -> {
            new Alert(Alert.AlertType.INFORMATION,
                    "Sharing " + selected.size() + " file(s) with " + recipient + "...").showAndWait();
            shareRecipientField.clear();
        });
    }
}
