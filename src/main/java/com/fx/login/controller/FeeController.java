package com.fx.login.controller;

import com.fx.login.config.SessionContext;
import com.fx.login.model.FeeEntity;
import com.fx.login.model.User;
import com.fx.login.service.FeeService;
import com.fx.login.MainApplication;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FeeController {
    @FXML
    private Button addFeeButton;

    @FXML
    private Button updateFeeButton;

    @FXML
    private Button deleteFeeButton;

    @FXML
    private TableView<FeeEntity> feeTable;
    @FXML
    private TableColumn<FeeEntity, Number> idColumn;
    @FXML
    private TableColumn<FeeEntity, String> feeNameColumn;
    @FXML
    private TableColumn<FeeEntity, String> amountDueColumn;
    @FXML
    private TableColumn<FeeEntity, String> monthlyFeeColumn;
    @FXML
    private TableColumn<FeeEntity, Void> unpaidHouseholdsColumn;
    @FXML
    private TextField searchField;

    private final ObservableList<FeeEntity> feesUiList = FXCollections.observableArrayList();
    private FeeService feeService;

    User currentUser;
    @FXML
    public void initialize() {
        // Lấy FeeService từ Spring Context
        if (MainApplication.getSpringContext() != null) {
            this.feeService = MainApplication.getSpringContext().getBean(FeeService.class);
        } else {
            // Xử lý trường hợp context chưa sẵn sàng (ví dụ: hiển thị lỗi)
            System.err.println("SpringContext is null in FeeController.initialize()");
            showAlert("Lỗi hệ thống", "Không thể khởi tạo dịch vụ quản lý khoản thu.");
            return; // Không thể tiếp tục nếu không có service
        }

        // Get current user first
        currentUser = SessionContext.getInstance().getCurrentUser();

        setupTableColumns();

        // Only load fees if user is admin or handle appropriately for different roles
        if (currentUser != null && currentUser.getRole() == User.Role.Admin) {
            loadFeesFromDb(); // Tải dữ liệu từ DB chỉ khi là admin
        } else {
            // For non-admin users, show empty list or load user-specific data
            feesUiList.clear();
            showAlert("Thông báo", "Bạn không có quyền xem danh sách phí.");
        }

        // Configure UI based on user role
        if (currentUser != null && currentUser.getRole() == User.Role.Resident) {
            addFeeButton.setVisible(false);
            addFeeButton.setDisable(true);
            updateFeeButton.setVisible(false);
            updateFeeButton.setDisable(true);
            deleteFeeButton.setVisible(false);
            deleteFeeButton.setDisable(true);
        }
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        feeNameColumn.setCellValueFactory(cellData -> cellData.getValue().feeNameProperty());
        amountDueColumn.setCellValueFactory(cellData -> cellData.getValue().amountDueProperty());
        monthlyFeeColumn.setCellValueFactory(cellData -> cellData.getValue().monthlyFeeProperty());
        // Cột "Xem DS chưa nộp"
        unpaidHouseholdsColumn.setCellFactory(col -> new TableCell<FeeEntity, Void>() {
            private final Button button = new Button("Xem");

            {
                button.setOnAction(event -> {
                    SessionContext.getInstance().setCurrentUser(currentUser);  // Tạo 1 session để lưu thông tin người đăng nhập
                    FeeEntity fee = getTableView().getItems().get(getIndex());
                    showUnpaidList(fee, "Danh sách hộ chưa nộp");
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(button);
                }
            }
        });
        feeTable.setItems(feesUiList);
    }

    private void loadFeesFromDb() {
        if (feeService == null) return; // Service chưa được inject/lấy
        try {
            List<FeeEntity> dbFees = feeService.findAll();
            dbFees.forEach(FeeEntity::syncProperties); // SỬA LỖI: syncProperties đã được thêm vào FeeEntity
            feesUiList.setAll(dbFees);
        } catch (SecurityException e) {
            // Handle security exception specifically
            System.err.println("Security error: " + e.getMessage());
            showAlert("Lỗi quyền truy cập", "Bạn không có quyền xem danh sách khoản thu.");
            feesUiList.clear();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi tải dữ liệu", "Không thể tải danh sách khoản thu từ cơ sở dữ liệu: " + e.getMessage());
        }
    }

    // Sửa đổi showFeeForm để trả về boolean (true nếu lưu, false nếu hủy)
    private boolean showFeeForm(FeeEntity fee, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/FeeForm.fxml"));
            Parent root = loader.load();
            FeeFormController controller = loader.getController();
            controller.setFee(fee); // Truyền đối tượng fee vào form

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.initModality(Modality.APPLICATION_MODAL); // Chặn tương tác với cửa sổ khác
            stage.setScene(new Scene(root));
            // controller.setStage(stage); // Nếu form controller cần tự đóng

            stage.showAndWait(); // Chờ cho đến khi form đóng

            return controller.isSaved(); // Trả về trạng thái lưu từ form controller

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi giao diện", "Không thể mở form thông tin khoản thu.");
            return false;
        }
    }

    // Sửa đổi showFeeForm để trả về boolean (true nếu lưu, false nếu hủy)
    private boolean showUnpaidList(FeeEntity fee, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/UnpaidList.fxml"));
            Parent root = loader.load();
            UnpaidListController controller = loader.getController();
            controller.setFee(fee); // Truyền đối tượng fee vào form
            // SỬA LỖI: setUnpaidHouseholds nhận List, không phải String
            fee.setUnpaidHouseholdsCount(String.valueOf(controller.updateUnpaidCountByFee(fee.getId())));
            controller.search(String.valueOf(fee.getId()));
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.initModality(Modality.APPLICATION_MODAL); // Chặn tương tác với cửa sổ khác
            stage.setScene(new Scene(root));
            // controller.setStage(stage); // Nếu form controller cần tự đóng

            stage.showAndWait(); // Chờ cho đến khi form đóng

            return controller.isSaved(); // Trả về trạng thái lưu từ form controller

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi giao diện", "Không thể mở form thông tin khoản thu.");
            return false;
        }
    }

    @FXML
    private void onSearch() {
        String keyword = searchField.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            if (currentUser != null && currentUser.getRole() == User.Role.Admin) {
                loadFeesFromDb(); // Tải lại toàn bộ danh sách nếu keyword rỗng và là admin
            }
            return;
        }

        // Lọc trên danh sách đang hiển thị (đã tải từ DB)
        // Hoặc bạn có thể tạo query tìm kiếm trong FeeService nếu muốn tìm trực tiếp từ DB
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
                            (fee.getMonthlyFee() != null && fee.getMonthlyFee().toLowerCase().contains(keyword)) ||
                            // SỬA LỖI: getUnpaidHouseholds() trả về List, không thể toLowerCase()
                            (fee.getUnpaidHouseholds() != null && String.valueOf(fee.getUnpaidHouseholdCount()).toLowerCase().contains(keyword));
                })
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        feeTable.setItems(filteredList);
    }

    @FXML
    private void onAdd() {
        // Check if user has permission to add fees
        if (currentUser == null || currentUser.getRole() != User.Role.Admin) {
            showAlert("Lỗi quyền truy cập", "Chỉ admin mới có quyền thêm khoản thu.");
            return;
        }

        FeeEntity newFee = new FeeEntity(); // Tạo đối tượng trống cho form
        boolean saved = showFeeForm(newFee, "Thêm khoản thu mới");

        if (saved) {
            try {
                // Không cần setId, DB sẽ tự tạo
                FeeEntity savedFee = feeService.createFee(newFee);
                savedFee.syncProperties(); // Đồng bộ properties (bao gồm ID mới)
                feesUiList.add(savedFee);
                feeTable.getSelectionModel().select(savedFee);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Lỗi lưu trữ", "Không thể thêm khoản thu: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onUpdate() {
        // Check if user has permission to update fees
        if (currentUser == null || currentUser.getRole() != User.Role.Admin) {
            showAlert("Lỗi quyền truy cập", "Chỉ admin mới có quyền cập nhật khoản thu.");
            return;
        }

        FeeEntity selected = feeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Chưa chọn khoản thu", "Hãy chọn một khoản thu để cập nhật.");
            return;
        }

        // Tạo bản sao để nếu người dùng hủy thì không ảnh hưởng object gốc trong list
        // Hoặc truyền trực tiếp selected và FeeFormController sẽ sửa đổi nó.
        // Để đơn giản, truyền trực tiếp và form sẽ cập nhật các trường của 'selected'
        boolean saved = showFeeForm(selected, "Cập nhật thông tin khoản thu");

        if (saved) {
            try {
                // 'selected' đã được cập nhật bởi form
                FeeEntity updatedFee = feeService.updateFee(selected.getId(), selected);
                updatedFee.syncProperties();

                // Cập nhật item trong list
                int index = feesUiList.indexOf(selected); // Tìm bằng object reference
                if (index != -1) {
                    feesUiList.set(index, updatedFee); // Thay thế bằng object đã được DB cập nhật
                } else {
                    loadFeesFromDb(); // Fallback nếu không tìm thấy (hiếm khi xảy ra)
                }
                feeTable.refresh(); // Đảm bảo TableView cập nhật
                feeTable.getSelectionModel().select(updatedFee);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Lỗi cập nhật", "Không thể cập nhật thông tin khoản thu: " + e.getMessage());
            }
        } else if (!saved && selected != null) {
            // Nếu người dùng cancel, và nếu form đã sửa đổi 'selected',
            // ta cần nạp lại từ DB để khôi phục giá trị gốc.
            // Cách tốt hơn là form làm việc trên một bản copy.
            // Hiện tại, để đơn giản, ta refresh bảng, có thể không đủ nếu 'selected' đã bị thay đổi
            feeTable.refresh();
            // Hoặc nạp lại item đó từ DB
            feeService.findById(selected.getId()).ifPresent(original -> {
                original.syncProperties();
                int index = feesUiList.indexOf(selected);
                if (index != -1) feesUiList.set(index, original);
            });
        }
    }

    @FXML
    private void onDelete() {
        // Check if user has permission to delete fees
        if (currentUser == null || currentUser.getRole() != User.Role.Admin) {
            showAlert("Lỗi quyền truy cập", "Chỉ admin mới có quyền xóa khoản thu.");
            return;
        }

        FeeEntity selected = feeTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Chưa chọn khoản thu", "Hãy chọn một khoản thu để xóa.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn có chắc muốn xóa khoản thu: " + selected.getFeeName() + "?",
                ButtonType.YES, ButtonType.NO);
        confirmAlert.setTitle("Xác nhận xóa");
        confirmAlert.setHeaderText(null);
        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.YES) {
            try {
                feeService.deleteFee(selected.getId());
                feesUiList.remove(selected);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Lỗi xóa", "Không thể xóa khoản thu: " + e.getMessage());
            }
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    // Overload cho cảnh báo đơn giản
    private void showAlert() {
        showAlert("Thông báo","Hãy chọn một khoản thu để thực hiện thao tác.");
    }
}