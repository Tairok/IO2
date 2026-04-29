package com.app.client.controller;

/**
 * Admin settings list controller.
 */

import com.app.client.model.Setting;
import com.app.client.service.SettingsService;
import com.app.client.network.Client;
import com.app.client.service.CommandService;
import com.app.client.utils.AppLogger;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

import static com.app.client.Config.FXML_ADMIN_PATH;
import static com.app.client.utils.DialogUtil.showError;

public class SettingsController {

    @FXML private TableView<Setting> permissionTable;
    @FXML private TableColumn<Setting, Integer> colPermId;
    @FXML private TableColumn<Setting, String>  colPermName;
    @FXML private TableColumn<Setting, String>  colPermDesc;
    @FXML private TableColumn<Setting, Integer> colPermUserId;

    private Client          client;
    private CommandService  cmdService;
    private SettingsService service;

    @FXML
    public void initialize() {
        colPermId    .setCellValueFactory(new PropertyValueFactory<>("userId"));
        colPermName  .setCellValueFactory(new PropertyValueFactory<>("displayName"));
        colPermDesc  .setCellValueFactory(new PropertyValueFactory<>("backgroundColor"));
        colPermUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));

        try {
            client     = new Client();
            client.connect();
            cmdService = client.getService();
            service    = new SettingsService(cmdService);
        } catch (IOException e) {
            showError("Failed to connect to server", e);
            AppLogger.error("Failed to connect to server", e);

            return;
        }

        loadData();
    }



    @FXML
    private void onAdd(ActionEvent evt) throws IOException {
        openForm(null);
    }

    @FXML
    private void onEdit(ActionEvent evt) throws IOException {
        Setting sel = permissionTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        openForm(sel);
    }

    @FXML
    private void onDelete(ActionEvent evt) {
        Setting sel = permissionTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try {
            service.delete(sel.getUserId());
            loadData();
        } catch (IOException e) {
            showError("Error deleting setting", e);
        }
    }

    @FXML
    private void onRefresh() {
        loadData();
    }

    private void openForm(Setting toEdit) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(FXML_ADMIN_PATH + "settings_form.fxml")
        );
        Parent root = loader.load();

        SettingsFormController ctrl = loader.getController();
        ctrl.setServices(cmdService);
        if (toEdit != null) {
            ctrl.initData(toEdit);
        }

        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(toEdit == null ? "New Setting" : "Edit Setting");
        dialog.setScene(new Scene(root));
        dialog.showAndWait();

        loadData();
    }

    private void loadData() {
        try {
            List<Setting> list = service.list();
            permissionTable.setItems(FXCollections.observableArrayList(list));
        } catch (IOException e) {
            showError("Error loading settings", e);
            AppLogger.error("Error loading settings", e);
        }
    }


}
