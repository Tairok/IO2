package com.app.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;


/**
 * Admin files view controller.
 */
public class AdminFilesController {
    @FXML private TableView<?> fileTable;
    @FXML private TableColumn<?, ?> colUser;
    @FXML private TableColumn<?, ?> colName;
    @FXML private TableColumn<?, ?> colSize;
    @FXML private TableColumn<?, ?> colDate;

    @FXML
    private void onRefresh() {
    }

    @FXML
    private void onDownload() {
    }

    @FXML
    private void onDelete() {
    }
}
