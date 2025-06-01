package com.fx.login.controller;

import com.fx.login.model.FeeEntity;
import com.fx.login.service.FeeService;
import com.fx.login.service.SessionService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Component
@FxmlView("/ui/admin-fee.fxml")
public class AdminFeeController implements Initializable {

    @Autowired
    private FeeService feeService;

    @Autowired
    private SessionService sessionService;

    @FXML private TableView<FeeEntity> feeTable;
    @FXML private TableColumn<FeeEntity, Long> idColumn;
    @FXML private TableColumn<FeeEntity, String> feeNameColumn;
    @FXML private TableColumn<FeeEntity, String> amountDueColumn;
    @FXML private TableColumn<FeeEntity, String> monthlyFeeColumn;
    @FXML private TableColumn<FeeEntity, String> unpaidHouseholdsColumn;

    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button addFeeButton;
    @FXML private Button updateFeeButton;
    @FXML private Button deleteFeeButton;

    private ObservableList<FeeEntity> feeList = FXCollections.observableArrayList();
    private FeeEntity selectedFee;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            System.out.println("AdminFeeController initialize() called");

            // Kiểm tra quyền admin
            if (sessionService == null || !sessionService.isAdmin()) {
                showAlert(Alert.AlertType.ERROR, "Lỗi quyền truy cập",
                        "Chỉ admin mới có quyền truy cập trang này.");
                return;
            }

            setupTableView();
            setupEventHandlers();
            loadFeeData();
            updateButtonStates();

            System.out.println("AdminFeeController initialized successfully");
        } catch (Exception e) {
            System.err.println("Error in AdminFeeController.initialize(): " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi khởi tạo", "Không thể khởi tạo giao diện: " + e.getMessage());
        }
    }

    private void setupTableView() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        feeNameColumn.setCellValueFactory(cellData -> cellData.getValue().feeNameProperty());
        amountDueColumn.setCellValueFactory(cellData -> cellData.getValue().amountDueProperty());
        monthlyFeeColumn.setCellValueFactory(cellData -> cellData.getValue().monthlyFeeProperty());

        // Cột số hộ chưa nộp
        unpaidHouseholdsColumn.setCellFactory(col -> new TableCell<FeeEntity, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    FeeEntity fee = (FeeEntity) getTableRow().getItem();
                    setText("Xem chi tiết");
                    setStyle("-fx-text-fill: #2196F3; -fx-cursor: hand;");
                }
            }
        });

        feeTable.setItems(feeList);
    }

    private void setupEventHandlers() {
        // Selection listener
        feeTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            selectedFee = newSelection;
            updateButtonStates();
        });

        // Double click listener
        feeTable.setOnMouseClicked(this::onTableDoubleClick);
    }

    private void loadFeeData() {
        try {
            List<FeeEntity> fees = feeService.findAll();
            fees.forEach(fee -> {
                try {
                    fee.syncToProperties();
                } catch (Exception e) {
                    System.err.println("Error syncing fee properties: " + e.getMessage());
                }
            });
            feeList.setAll(fees);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi tải dữ liệu",
                    "Không thể tải danh sách khoản phí: " + e.getMessage());
        }
    }

    private void refreshData() {
        loadFeeData();
        selectedFee = null;
        feeTable.getSelectionModel().clearSelection();
        updateButtonStates();
    }

    private void searchFees(String searchText) {
        try {
            ObservableList<FeeEntity> filteredList = feeList.stream()
                    .filter(fee -> {
                        boolean matchesId = false;
                        try {
                            Long id = Long.parseLong(searchText);
                            matchesId = fee.getId() != null && fee.getId().equals(id);
                        } catch (NumberFormatException ignored) {}
                        return matchesId ||
                                (fee.getFeeName() != null && fee.getFeeName().toLowerCase().contains(searchText)) ||
                                (fee.getAmountDue() != null && fee.getAmountDue().toLowerCase().contains(searchText)) ||
                                (fee.getMonthlyFee() != null && fee.getMonthlyFee().toLowerCase().contains(searchText));
                    })
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));
            feeTable.setItems(filteredList);
        } catch (Exception e) {
            System.err.println("Error in search: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onSearch() {
        try {
            String keyword = searchField.getText().trim().toLowerCase();
            if (keyword.isEmpty()) {
                refreshData();
                return;
            }
            searchFees(keyword);
        } catch (Exception e) {
            System.err.println("Error in onSearch: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onAdd() {
        try {
            // Tạo dialog để nhập thông tin khoản phí mới
            Dialog<FeeEntity> dialog = createFeeDialog("Thêm khoản phí mới", null);
            dialog.showAndWait().ifPresent(fee -> {
                try {
                    FeeEntity savedFee = feeService.createFee(fee);
                    refreshData();
                    showAlert(Alert.AlertType.INFORMATION, "Thành công",
                            "Đã thêm khoản phí: " + savedFee.getFeeName());
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi",
                            "Không thể thêm khoản phí: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Error in onAdd: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onUpdate() {
        try {
            if (selectedFee == null) {
                showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn khoản phí cần sửa.");
                return;
            }

            Dialog<FeeEntity> dialog = createFeeDialog("Sửa thông tin khoản phí", selectedFee);
            dialog.showAndWait().ifPresent(fee -> {
                try {
                    FeeEntity updatedFee = feeService.updateFee(selectedFee.getId(), fee);
                    refreshData();
                    showAlert(Alert.AlertType.INFORMATION, "Thành công",
                            "Đã cập nhật khoản phí: " + updatedFee.getFeeName());
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi",
                            "Không thể cập nhật khoản phí: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Error in onUpdate: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onDelete() {
        try {
            if (selectedFee == null) {
                showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn khoản phí cần xóa.");
                return;
            }

            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Xác nhận xóa");
            confirmAlert.setHeaderText("Bạn có chắc chắn muốn xóa khoản phí này?");
            confirmAlert.setContentText("Khoản phí: " + selectedFee.getFeeName() + "\nHành động này không thể hoàn tác.");

            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        feeService.deleteFee(selectedFee.getId());
                        refreshData();
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã xóa khoản phí thành công.");
                    } catch (Exception e) {
                        showAlert(Alert.AlertType.ERROR, "Lỗi",
                                "Không thể xóa khoản phí: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("Error in onDelete: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onRefresh() {
        try {
            if (searchField != null) {
                searchField.clear();
            }
            refreshData();
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã làm mới dữ liệu!");
        } catch (Exception e) {
            System.err.println("Error in onRefresh: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Dialog<FeeEntity> createFeeDialog(String title, FeeEntity existingFee) {
        Dialog<FeeEntity> dialog = new Dialog<>();
        dialog.setTitle(title);

        // Tạo form fields
        TextField feeNameField = new TextField();
        TextField amountDueField = new TextField();
        TextField monthlyFeeField = new TextField();
        CheckBox isActiveCheckBox = new CheckBox("Đang hoạt động");

        if (existingFee != null) {
            feeNameField.setText(existingFee.getFeeName());
            amountDueField.setText(existingFee.getAmountDue());
            monthlyFeeField.setText(existingFee.getMonthlyFee());
            isActiveCheckBox.setSelected(existingFee.getIsActive() != null ? existingFee.getIsActive() : true);
        } else {
            isActiveCheckBox.setSelected(true);
        }

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Tên khoản phí:"), 0, 0);
        grid.add(feeNameField, 1, 0);
        grid.add(new Label("Số tiền phải nộp:"), 0, 1);
        grid.add(amountDueField, 1, 1);
        grid.add(new Label("Phí hàng tháng:"), 0, 2);
        grid.add(monthlyFeeField, 1, 2);
        grid.add(isActiveCheckBox, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {
                    FeeEntity fee = existingFee != null ? existingFee : new FeeEntity();
                    fee.setFeeName(feeNameField.getText().trim());
                    fee.setAmountDue(amountDueField.getText().trim());
                    fee.setMonthlyFee(monthlyFeeField.getText().trim());
                    fee.setIsActive(isActiveCheckBox.isSelected());
                    // Sửa lỗi: dùng setCreatedDate thay vì setCreatedAt
                    if (existingFee == null) {
                        fee.setCreatedDate(LocalDateTime.now());
                    }
                    fee.setUpdatedDate(LocalDateTime.now());
                    return fee;
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Dữ liệu không hợp lệ: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

        return dialog;
    }

    private void onTableDoubleClick(MouseEvent event) {
        if (event.getClickCount() == 2 && selectedFee != null) {
            onUpdate();
        }
    }

    private void updateButtonStates() {
        boolean hasSelection = selectedFee != null;
        if (updateFeeButton != null) updateFeeButton.setDisable(!hasSelection);
        if (deleteFeeButton != null) deleteFeeButton.setDisable(!hasSelection);
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        try {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        } catch (Exception e) {
            System.err.println("Error showing alert: " + e.getMessage());
        }
    }
}