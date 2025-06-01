package com.fx.login.controller;

import com.fx.login.model.Resident;
import com.fx.login.model.SelectableResident;
import com.fx.login.model.User;
import com.fx.login.service.ResidentService;
import com.fx.login.service.UserService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Component
public class RecipientSelectionDialogController implements Initializable {

    @FXML private TextField searchField;
    @FXML private TableView<SelectableResident> residentTable;
    @FXML private TableColumn<SelectableResident, Boolean> selectColumn;
    @FXML private TableColumn<SelectableResident, Long> idColumn;
    @FXML private TableColumn<SelectableResident, String> nameColumn;
    @FXML private TableColumn<SelectableResident, String> emailColumn;
    @FXML private TableColumn<SelectableResident, String> phoneColumn;
    @FXML private TableColumn<SelectableResident, String> apartmentColumn;
    @FXML private Label selectionCountLabel;

    private ResidentService residentService;
    private UserService userService;

    private final ObservableList<SelectableResident> residentData = FXCollections.observableArrayList();
    private FilteredList<SelectableResident> filteredResidents;
    private List<Long> preSelectedIds = new ArrayList<>();

    // Necessary for Spring to initialize services when loading the dialog
    private static ApplicationContext applicationContext;

    public RecipientSelectionDialogController() {
        // No-arg constructor for FXML loader
    }

    @Autowired
    public RecipientSelectionDialogController(ResidentService residentService, UserService userService, ApplicationContext context) {
        this.residentService = residentService;
        this.userService = userService;
        RecipientSelectionDialogController.applicationContext = context;
    }

    public void setPreSelectedIds(List<Long> ids) {
        if (ids != null) {
            this.preSelectedIds = ids;
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize services if loaded via FXML
        if (residentService == null && applicationContext != null) {
            residentService = applicationContext.getBean(ResidentService.class);
            userService = applicationContext.getBean(UserService.class);
        }

        setupTableColumns();
        loadResidents();
        setupSearch();
    }

    private void setupTableColumns() {
        selectColumn.setCellValueFactory(param -> param.getValue().selectedProperty());
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        selectColumn.setEditable(true);

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        apartmentColumn.setCellValueFactory(new PropertyValueFactory<>("apartmentNumber"));

        // Make the table editable for the checkboxes
        residentTable.setEditable(true);
    }

    private void loadResidents() {
        try {
            List<Resident> residents = residentService.findAll();
            residentData.clear();

            for (Resident resident : residents) {
                SelectableResident selectableResident = new SelectableResident(resident);

                // Set as selected if in pre-selected IDs
                if (preSelectedIds.contains(resident.getId())) {
                    selectableResident.setSelected(true);
                }

                // Add listener to update selection count
                selectableResident.selectedProperty().addListener((obs, oldVal, newVal) -> {
                    updateSelectionCount();
                });

                residentData.add(selectableResident);
            }

            filteredResidents = new FilteredList<>(residentData, p -> true);
            residentTable.setItems(filteredResidents);
            updateSelectionCount();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Lỗi khi tải danh sách cư dân: " + e.getMessage());
        }
    }

    private void setupSearch() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            String searchText = newValue.toLowerCase().trim();
            filteredResidents.setPredicate(resident -> {
                if (searchText.isEmpty()) {
                    return true;
                }

                return String.valueOf(resident.getId()).contains(searchText) ||
                        (resident.getFullName() != null && resident.getFullName().toLowerCase().contains(searchText)) ||
                        (resident.getEmail() != null && resident.getEmail().toLowerCase().contains(searchText)) ||
                        (resident.getPhone() != null && resident.getPhone().toLowerCase().contains(searchText)) ||
                        (resident.getApartmentNumber() != null && resident.getApartmentNumber().toLowerCase().contains(searchText));
            });
        });
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        // Search is handled by listener already
    }

    @FXML
    private void handleSelectAll(ActionEvent event) {
        for (SelectableResident resident : filteredResidents) {
            resident.setSelected(true);
        }
    }

    @FXML
    private void handleDeselectAll(ActionEvent event) {
        for (SelectableResident resident : filteredResidents) {
            resident.setSelected(false);
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeDialog(event);
    }

    @FXML
    private void handleConfirm(ActionEvent event) {
        closeDialog(event);
    }

    private void closeDialog(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private void updateSelectionCount() {
        long count = residentData.stream().filter(SelectableResident::isSelected).count();
        selectionCountLabel.setText(String.format("Đã chọn: %d người", count));
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public List<Long> getSelectedResidentIds() {
        return residentData.stream()
                .filter(SelectableResident::isSelected)
                .map(SelectableResident::getId)
                .collect(Collectors.toList());
    }

    public List<User> getSelectedRecipients() {
        List<Long> selectedIds = getSelectedResidentIds();

        if (selectedIds.isEmpty()) {
            return new ArrayList<>();
        }

        return userService.findResidentsByIds(selectedIds);
    }
}