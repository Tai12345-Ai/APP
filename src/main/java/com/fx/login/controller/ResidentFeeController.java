package com.fx.login.controller;

import com.fx.login.model.FeeEntity;
import com.fx.login.model.UnpaidEntity;
import com.fx.login.model.User;
import com.fx.login.service.FeeService;
import com.fx.login.service.SessionService;
import com.fx.login.service.UnpaidService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Component
@FxmlView("/ui/resident-fee.fxml")
public class ResidentFeeController implements Initializable {

    @Autowired
    private FeeService feeService;

    @Autowired
    private UnpaidService unpaidService;

    @Autowired
    private SessionService sessionService;

    @FXML private TableView<FeeEntity> feeTable;
    @FXML private TableColumn<FeeEntity, Number> idColumn;
    @FXML private TableColumn<FeeEntity, String> feeNameColumn;
    @FXML private TableColumn<FeeEntity, String> amountDueColumn;
    @FXML private TableColumn<FeeEntity, String> monthlyFeeColumn;
    @FXML private TableColumn<FeeEntity, String> statusColumn;
    @FXML private TextField searchField;
    @FXML private Label infoLabel;
    @FXML private Label totalFeesLabel;
    @FXML private Label unpaidCountLabel;
    @FXML private Label paidCountLabel;

    private final ObservableList<FeeEntity> feesUiList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            System.out.println("ResidentFeeController initialize() called");

            // Kiểm tra sessionService có null không
            if (sessionService == null) {
                System.err.println("SessionService is null!");
                showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Không thể khởi tạo session service");
                return;
            }

            // Kiểm tra quyền resident
            if (!sessionService.isResident()) {
                showAlert(Alert.AlertType.ERROR, "Lỗi quyền truy cập",
                        "Chỉ cư dân mới có quyền truy cập trang này.");
                return;
            }

            User currentUser = sessionService.getCurrentUser();
            if (currentUser != null) {
                String residentName = currentUser.getResidentFullName() != null ?
                        currentUser.getResidentFullName() : currentUser.getFullname();
                String apartmentNumber = currentUser.getApartmentNumber() != null ?
                        currentUser.getApartmentNumber() : "N/A";

                infoLabel.setText("Cư dân: " + residentName + " - Căn hộ: " + apartmentNumber);
            }

            setupTableColumns();
            loadMyFees();
            updateStatistics();

            System.out.println("ResidentFeeController initialized successfully");
        } catch (Exception e) {
            System.err.println("Error in ResidentFeeController.initialize(): " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi khởi tạo", "Không thể khởi tạo giao diện: " + e.getMessage());
        }
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        feeNameColumn.setCellValueFactory(cellData -> cellData.getValue().feeNameProperty());
        amountDueColumn.setCellValueFactory(cellData -> cellData.getValue().amountDueProperty());
        monthlyFeeColumn.setCellValueFactory(cellData -> cellData.getValue().monthlyFeeProperty());

        // Cột trạng thái thanh toán cho resident
        statusColumn.setCellFactory(col -> new TableCell<FeeEntity, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                } else {
                    FeeEntity fee = (FeeEntity) getTableRow().getItem();
                    String status = getPaymentStatus(fee);
                    setText(status);

                    if ("Đã thanh toán".equals(status)) {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                    }
                }
            }
        });

        feeTable.setItems(feesUiList);
    }

    private String getPaymentStatus(FeeEntity fee) {
        try {
            List<UnpaidEntity> myUnpaid = unpaidService.findMyUnpaidFees();
            boolean hasUnpaid = myUnpaid.stream()
                    .anyMatch(unpaid -> unpaid.getFeeID().equals(fee.getId()) &&
                            "PENDING".equals(unpaid.getStatus()));
            return hasUnpaid ? "Chưa thanh toán" : "Đã thanh toán";
        } catch (Exception e) {
            System.err.println("Error getting payment status: " + e.getMessage());
            return "Không xác định";
        }
    }

    private void loadMyFees() {
        try {
            // Lấy tất cả các loại phí có trong hệ thống
            List<FeeEntity> allFees = feeService.findAll();
            // Sync JavaFX properties
            allFees.forEach(fee -> {
                try {
                    fee.syncToProperties();
                } catch (Exception e) {
                    System.err.println("Error syncing fee properties: " + e.getMessage());
                }
            });
            feesUiList.setAll(allFees);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi tải dữ liệu",
                    "Không thể tải danh sách khoản phí: " + e.getMessage());
        }
    }

    private void updateStatistics() {
        try {
            List<FeeEntity> allFees = feeService.findAll();
            List<UnpaidEntity> myUnpaid = unpaidService.findMyUnpaidFees();

            int totalFees = allFees.size();
            int unpaidCount = (int) myUnpaid.stream()
                    .filter(unpaid -> "PENDING".equals(unpaid.getStatus()))
                    .count();
            int paidCount = totalFees - unpaidCount;

            if (totalFeesLabel != null) totalFeesLabel.setText(String.valueOf(totalFees));
            if (unpaidCountLabel != null) unpaidCountLabel.setText(String.valueOf(unpaidCount));
            if (paidCountLabel != null) paidCountLabel.setText(String.valueOf(paidCount));

        } catch (Exception e) {
            System.err.println("Error updating statistics: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onSearch() {
        try {
            String keyword = searchField.getText().trim().toLowerCase();
            if (keyword.isEmpty()) {
                loadMyFees();
                return;
            }

            ObservableList<FeeEntity> filteredList = feesUiList.stream()
                    .filter(fee -> {
                        boolean matchesId = false;
                        try {
                            Long id = Long.parseLong(keyword);
                            matchesId = fee.getId() != null && fee.getId().equals(id);
                        } catch (NumberFormatException ignored) {}
                        return matchesId ||
                                (fee.getFeeName() != null && fee.getFeeName().toLowerCase().contains(keyword)) ||
                                (fee.getAmountDue() != null && fee.getAmountDue().toLowerCase().contains(keyword)) ||
                                (fee.getMonthlyFee() != null && fee.getMonthlyFee().toLowerCase().contains(keyword));
                    })
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));
            feeTable.setItems(filteredList);
        } catch (Exception e) {
            System.err.println("Error in search: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onRefresh() {
        try {
            // Xóa nội dung tìm kiếm
            if (searchField != null) {
                searchField.clear();
            }

            // Xóa selection hiện tại
            if (feeTable != null) {
                feeTable.getSelectionModel().clearSelection();
            }

            // Tải lại dữ liệu từ database
            loadMyFees();

            // Cập nhật thống kê
            updateStatistics();

            // Hiển thị thông báo thành công
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã làm mới dữ liệu!");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể làm mới dữ liệu: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        try {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        } catch (Exception e) {
            System.err.println("Error showing alert: " + e.getMessage());
        }
    }
}