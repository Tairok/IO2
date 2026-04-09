package com.app.client.controller;

import com.app.client.model.FileEntry;
import com.app.client.service.FileService;
import com.app.client.utils.AppLogger;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DownloadController {



    @FXML private Label     destLabel;
    @FXML private TableView<FileEntry> fileTable;
    @FXML private TableColumn<FileEntry,String> nameCol;
    @FXML private TableColumn<FileEntry,Number> sizeCol;
    @FXML private TableColumn<FileEntry,String> dateCol;
    @FXML private VBox      progressContainer;
    @FXML private Button downloadButton;


    private String      username;
    private FileService fileService;
    private File        destDir;
    // ExecutorService runs file transfer tasks in background threads to keep UI responsive
private final ExecutorService executor = Executors.newCachedThreadPool();

    @FXML
    public void initialize() {
        nameCol.setCellValueFactory(c -> c.getValue().filenameProperty());
        sizeCol.setCellValueFactory(c -> c.getValue().sizeProperty());
        dateCol.setCellValueFactory(c -> c.getValue().lastModifiedProperty());
        fileTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    @FXML
    private void onRefreshList() {
        try {
            List<FileEntry> remote = fileService.listFiles(username);
            fileTable.setItems(FXCollections.observableArrayList(remote));
            downloadButton.setDisable(remote.isEmpty());
        } catch (Exception e) {
            showAlert("Error", "Failed to fetch file list:\n" + e);
        }
    }

    @FXML
    private void onStartDownload() {
        // 1) Pick destination folder once
        if (destDir == null) {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Wybierz folder docelowy");
            Window w = fileTable.getScene().getWindow();
            destDir = dc.showDialog(w);
            if (destDir != null) {
                destLabel.setText("Folder docelowy: " + destDir.getAbsolutePath());
            } else {
                return;
            }
        }

        // 2) Grab selected files
        List<FileEntry> toDl = fileTable.getSelectionModel().getSelectedItems();
        if (toDl.isEmpty()) return;

        // 3) Clear old progress bars
        progressContainer.getChildren().clear();
        downloadButton.setDisable(true);

        // 4) Spin up one Task per file
        for (FileEntry fe : toDl) {
            HBox row = new HBox(10);
            Label name = new Label(fe.getFilename());
            ProgressBar pb = new ProgressBar(0);
            row.getChildren().addAll(name, pb);
            progressContainer.getChildren().add(row);

            Task<Boolean> task = new Task<>() {
                @Override protected Boolean call() throws Exception {
                    File out = new File(destDir, fe.getFilename());
                    return fileService.downloadFile(
                            username,
                            fe.getFilename(),
                            out,
                            (rec, tot) -> updateProgress(rec, tot)
                    );
                }
            };


            // 5) Bind progress and color‐code on finish
            pb.progressProperty().bind(task.progressProperty());
            task.setOnSucceeded(e -> {
                boolean ok = task.getValue();
                name.setStyle(ok
                        ? "-fx-text-fill: green;"
                        : "-fx-text-fill: red;");
                if (ok) onRefreshList();
            });
            task.setOnFailed(e -> {
                name.setStyle("-fx-text-fill: red;");
                AppLogger.error("Download failed for " + fe.getFilename(),
                        task.getException());
            });

            executor.submit(task);
        }
    }

    private void showAlert(String title, String text) {
        Alert a = new Alert(Alert.AlertType.ERROR, text, ButtonType.OK);
        a.setTitle(title);
        a.showAndWait();
    }

}
