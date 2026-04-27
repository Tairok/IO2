package com.app.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class AdminFilesController {
    @FXML private TableView<?> fileTable;
    @FXML private TableColumn<?, ?> colUser;
    @FXML private TableColumn<?, ?> colName;
    @FXML private TableColumn<?, ?> colSize;
    @FXML private TableColumn<?, ?> colDate;

    @FXML
    private void onRefresh() {
        // TODO: implement refresh logic
    }

    @FXML
    private void onDownload() {
        // TODO: implement download logic
    }

    @FXML
    private void onDelete() {
        // TODO: implement delete logic
    }
}
