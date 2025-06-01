package com.fx.login.controller;

import com.fx.login.config.SessionContext;
import com.fx.login.model.FeeEntity;
import com.fx.login.MainApplication;
import com.fx.login.model.UnpaidEntity;
import com.fx.login.model.User;
import com.fx.login.service.UnpaidService;
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

public class UnpaidListController {
    @FXML
    private Button addFormButton;

    @FXML
    private Button updateFormButton;

    @FXML
    private Button deleteFormButton;
    @FXML
    private TextField feeNameField;

    private String amountDueField;
    @FXML
    private TextField monthlyFeeField;
    @FXML
    private TextField unpaidHouseholdsField;

    @FXML
    private TableView<UnpaidEntity> unpaidTable;
    @FXML
    private TableColumn<UnpaidEntity, Number> idColumn;
    @FXML
    private TableColumn<UnpaidEntity, String> residentNameColumn;
    @FXML
    private TableColumn<UnpaidEntity, String> apartmentNameColumn;
    @FXML
    private TableColumn<UnpaidEntity, String> totalPaymentColumn;
    @FXML
    private TableColumn<UnpaidEntity, String> dueDateColumn;
    @FXML
    private TextField searchField;

    private FeeEntity fee;
    private boolean saved = false; // Cờ để biết form có được lưu không
    private boolean isUpdateMode = false; // Cờ để biết có cần tắt editable trong hàm update không

    public void setFee(FeeEntity fee) {
        this.fee = fee;
        this.saved = false; // Reset cờ khi set resident mới
        if (fee != null) {
            feeNameField.setText(fee.getFeeName());
            amountDueField = fee.getAmountDue();
            if(amountDueField.equals("Bấm \"Xem\" để biết chính xác")) isUpdateMode = true;
            monthlyFeeField.setText(fee.getMonthlyFee());
            
            // SỬA LỖI: Convert List thành String
            List<Object> unpaidHouseholds = fee.getUnpaidHouseholds();
            if (unpaidHouseholds != null) {
                unpaidHouseholdsField.setText(String.valueOf(unpaidHouseholds.size()));
            } else {
                unpaidHouseholdsField.setText("0");
            }
            
            feeNameField.setEditable(false);
            monthlyFeeField.setEditable(false);
            unpaidHouseholdsField.setEditable(false);
        } else {
            // Nếu là thêm mới, resident có thể là new ResidentEntity() rỗng
            // hoặc bạn có thể khởi tạo một resident mới ở đây nếu cần
            this.fee = new FeeEntity(); // Đảm bảo resident không null
            clearFields();
        }
    }

    public boolean isSaved() {
        return saved;
    }

    private void clearFields() {
        feeNameField.clear();
        amountDueField = null;
        monthlyFeeField.clear();
        unpaidHouseholdsField.clear();
    }

    private final ObservableList<UnpaidEntity> unpaidsUiList = FXCollections.observableArrayList();
    private UnpaidService unpaidService;

    User currentUser;
    @FXML
    public void initialize() {
        // Lấy feeService từ Spring Context
        if (MainApplication.getSpringContext() != null) {
            this.unpaidService = MainApplication.getSpringContext().getBean(UnpaidService.class);
        } else {
            // Xử lý trường hợp context chưa sẵn sàng (ví dụ: hiển thị lỗi)
            System.err.println("SpringContext is null in ResidentController.initialize()");
            showAlert("Lỗi hệ thống", "Không thể khởi tạo dịch vụ quản lý cư dân.");
            return; // Không thể tiếp tục nếu không có service
        }

        setupTableColumns();
        loadUnpaidsFromDb(); // Tải dữ liệu từ DB
        if(fee != null) search(String.valueOf(fee.getId()));

        currentUser = SessionContext.getInstance().getCurrentUser();    //Lấy thông tin của session
        if (currentUser != null && currentUser.getRole() == User.Role.Resident) {
            addFormButton.setVisible(false);
            addFormButton.setDisable(true);
            updateFormButton.setVisible(false);
            updateFormButton.setDisable(true);
            deleteFormButton.setVisible(false);
            deleteFormButton.setDisable(true);
        }
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        residentNameColumn.setCellValueFactory(cellData -> cellData.getValue().residentNameProperty());
        apartmentNameColumn.setCellValueFactory(cellData -> cellData.getValue().apartmentNameProperty());
        totalPaymentColumn.setCellValueFactory(cellData -> cellData.getValue().totalPaymentProperty());
        dueDateColumn.setCellValueFactory(cellData -> cellData.getValue().dueDateProperty());
        unpaidTable.setItems(unpaidsUiList);
    }

    private void loadUnpaidsFromDb() {
        if (unpaidService == null) return; // Service chưa được inject/lấy
        try {
            List<UnpaidEntity> dbUnpaids = unpaidService.findAll();
            dbUnpaids.forEach(UnpaidEntity::syncProperties); // QUAN TRỌNG: Đồng bộ properties sau khi load
            unpaidsUiList.setAll(dbUnpaids);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi tải dữ liệu", "Không thể tải danh sách cư dân từ cơ sở dữ liệu: " + e.getMessage());
        }
    }

    public long updateUnpaidCountByFee(Long selectedFeeId) {
        long count = unpaidsUiList.stream()
                .filter(unpaid -> unpaid.getFeeID() != null && unpaid.getFeeID().equals(selectedFeeId))
                .count();
        unpaidHouseholdsField.setText(String.valueOf(count));
        return count;
    }

    @FXML
    private void onSearch() {
        String keyword = searchField.getText().trim().toLowerCase();
        search(keyword, String.valueOf(fee.getId()));
    }

    public void search(String keyword1, String keyword2) {
        if ((keyword1 == null || keyword1.isEmpty()) && (keyword2 == null || keyword2.isEmpty())) {
            loadUnpaidsFromDb(); // Tải lại toàn bộ danh sách nếu cả hai keyword rỗng
            return;
        }

        // Lọc danh sách hiển thị
        ObservableList<UnpaidEntity> filteredList = unpaidsUiList.stream()
                .filter(unpaid -> {
                    boolean match1 = matchesKeyword1(unpaid, keyword1);
                    boolean match2 = matchesKeyword2(unpaid, keyword2);
                    return match1 && match2;
                })
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        unpaidTable.setItems(filteredList);
    }

    // Hàm hỗ trợ kiểm tra một keyword có khớp với entity không
    private boolean matchesKeyword1(UnpaidEntity unpaid, String keyword) {
        if (keyword == null || keyword.isEmpty()) return true; // Nếu rỗng thì xem như luôn khớp

        keyword = keyword.toLowerCase();
        try {
            Long id = Long.parseLong(keyword);
            if (unpaid.getId() != null && unpaid.getId().equals(id)) return true;
        } catch (NumberFormatException ignored) {}

        return (unpaid.getResidentName() != null && unpaid.getResidentName().toLowerCase().contains(keyword)) ||
                (unpaid.getApartmentName() != null && unpaid.getApartmentName().toLowerCase().contains(keyword)) ||
                (unpaid.getTotalPayment() != null && unpaid.getTotalPayment().toLowerCase().contains(keyword)) ||
                (unpaid.getDueDate() != null && unpaid.getDueDate().toLowerCase().contains(keyword));
    }

    // Hàm hỗ trợ kiểm tra một keyword có khớp với feeID không
    private boolean matchesKeyword2(UnpaidEntity unpaid, String keyword) {
        if (keyword == null || keyword.isEmpty()) return true; // Nếu rỗng thì xem như luôn khớp

        keyword = keyword.toLowerCase();
        try {
            Long id = Long.parseLong(keyword);
            if (unpaid.getFeeID() != null && unpaid.getFeeID().equals(id)) return true;
        } catch (NumberFormatException ignored) {}

        return false;
    }

    public void search(String keyword) {
        if (keyword.isEmpty()) {
            loadUnpaidsFromDb(); // Tải lại toàn bộ danh sách nếu keyword rỗng
            return;
        }

        ObservableList<UnpaidEntity> filteredList = unpaidsUiList.stream()
                .filter(unpaid -> {
                    boolean matchesId = false;
                    try {
                        Long id = Long.parseLong(keyword);
                        matchesId = unpaid.getFeeID() != null && unpaid.getFeeID().equals(id);
                    } catch (NumberFormatException ignored) {}
                    return matchesId;
                })
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        unpaidTable.setItems(filteredList);
    }

    @FXML
    private void onAdd() {
        UnpaidEntity newUnpaid = new UnpaidEntity(); // Tạo đối tượng trống cho form
        // Lấy trực tiếp từ fee để tránh TH load lại bị sai do ng dùng nhập lỗi(//Đã fix k cho người dùng thay đổi)
        Long s = fee.getId();
        newUnpaid.setFeeID(s);
        if(amountDueField != null) newUnpaid.setTotalPayment(amountDueField);
//        if(amountDueField.equals("Bấm \"Xem\" để biết chính xác")) newUnpaid.setTotalPayment("");
        boolean saved = showUnpaidForm(newUnpaid, "Thêm khoản phí mới");

        if (saved) {
            try {
                // Không cần setId, DB sẽ tự tạo
                UnpaidEntity savedUnpaid = unpaidService.createUnpaid(newUnpaid);
                savedUnpaid.syncProperties(); // Đồng bộ properties (bao gồm ID mới)
                unpaidsUiList.add(savedUnpaid);
                unpaidTable.getSelectionModel().select(savedUnpaid);
                updateUnpaidCountByFee(s);
                search(String.valueOf(s));
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Lỗi lưu trữ", "Không thể thêm khoản phí: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onUpdate() {
        UnpaidEntity selected = unpaidTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Chưa chọn khoản phí", "Hãy chọn một khoản phí để cập nhật.");
            return;
        }
        String s = String.valueOf(selected.getFeeID());

        // Tạo bản sao để nếu người dùng hủy thì không ảnh hưởng object gốc trong list
        // Hoặc truyền trực tiếp selected và ResidentFormController sẽ sửa đổi nó.
        // Để đơn giản, truyền trực tiếp và form sẽ cập nhật các trường của 'selected'
        boolean saved = showUnpaidForm(selected, "Cập nhật thông tin khoản phí");

        if (saved) {
            try {
                // 'selected' đã được cập nhật bởi form
                UnpaidEntity updatedUnpaid = unpaidService.updateUnpaid(selected.getId(), selected);
                updatedUnpaid.syncProperties();

                // Cập nhật item trong list
                int index = unpaidsUiList.indexOf(selected); // Tìm bằng object reference
                if (index != -1) {
                    unpaidsUiList.set(index, updatedUnpaid); // Thay thế bằng object đã được DB cập nhật
                } else {
                    loadUnpaidsFromDb(); // Fallback nếu không tìm thấy (hiếm khi xảy ra)
                }
                unpaidTable.refresh(); // Đảm bảo TableView cập nhật
                unpaidTable.getSelectionModel().select(updatedUnpaid);
                updateUnpaidCountByFee(Long.parseLong(s));
                search(s);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Lỗi cập nhật", "Không thể cập nhật thông tin khoản phí: " + e.getMessage());
            }
        } else if (!saved && selected != null) {
            // Nếu người dùng cancel, và nếu form đã sửa đổi 'selected',
            // ta cần nạp lại từ DB để khôi phục giá trị gốc.
            // Cách tốt hơn là form làm việc trên một bản copy.
            // Hiện tại, để đơn giản, ta refresh bảng, có thể không đủ nếu 'selected' đã bị thay đổi
            unpaidTable.refresh();
            // Hoặc nạp lại item đó từ DB
            unpaidService.findById(selected.getId()).ifPresent(original -> {
                original.syncProperties();
                int index = unpaidsUiList.indexOf(selected);
                if (index != -1) unpaidsUiList.set(index, original);
            });
        }
    }

    @FXML
    private void onDelete() {
        UnpaidEntity selected = unpaidTable.getSelectionModel().getSelectedItem();
        Long s = selected.getFeeID();
        if (selected == null) {
            showAlert("Chưa chọn khoản phí", "Hãy chọn một khoản phí để xóa.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn có chắc muốn xóa khoản phí của căn hộ: " + selected.getApartmentName() + "?",
                ButtonType.YES, ButtonType.NO);
        confirmAlert.setTitle("Xác nhận xóa");
        confirmAlert.setHeaderText(null);
        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.YES) {
            try {
                unpaidService.deleteUnpaid(selected.getId());
                unpaidsUiList.remove(selected);
                updateUnpaidCountByFee(s);
                search(String.valueOf(s));
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Lỗi xóa", "Không thể xóa khoản phí: " + e.getMessage());
            }
        }
    }

    private boolean showUnpaidForm(UnpaidEntity unpaid, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/UnpaidForm.fxml"));
            Parent root = loader.load();
            UnpaidFormController controller = loader.getController();
            controller.setIsUpdateMode(isUpdateMode);
            controller.setUnpaid(unpaid); // Truyền đối tượng unpaid vào form

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.initModality(Modality.APPLICATION_MODAL); // Chặn tương tác với cửa sổ khác
            stage.setScene(new Scene(root));
            // controller.setStage(stage); // Nếu form controller cần tự đóng

            stage.showAndWait(); // Chờ cho đến khi form đóng

            return controller.isSaved(); // Trả về trạng thái lưu từ form controller

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi giao diện", "Không thể mở form thông tin các khoản phí.");
            return false;
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
        showAlert("Thông báo","Hãy chọn một khoản phí để thực hiện thao tác.");
    }

}