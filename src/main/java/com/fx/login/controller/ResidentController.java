package com.fx.login.controller;

import com.fx.login.MainApplication;
import com.fx.login.config.SessionContext;
import com.fx.login.model.User;
import com.fx.login.service.ResidentService;
import com.fx.login.model.Resident;
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

public class ResidentController {
    @FXML
    private Button AddResident;

    @FXML
    private Button UpdateResident;

    @FXML
    private Button DeleteResident;

    @FXML
    private TableView<Resident> residentTable;
    @FXML
    private TableColumn<Resident, Number> idColumn;
    @FXML
    private TableColumn<Resident, String> fullNameColumn;
    @FXML
    private TableColumn<Resident, String> emailColumn;
    @FXML
    private TableColumn<Resident, String> phoneColumn;
    @FXML
    private TableColumn<Resident, String> apartmentNumberColumn;
    @FXML
    private TextField searchField;

    private final ObservableList<Resident> residentsUiList = FXCollections.observableArrayList();
    private ResidentService residentService;
    // private long nextId = 3; // Bỏ nextId thủ công
    User currentUser;
    @FXML
    public void initialize() {
        // Lấy ResidentService từ Spring Context
        if (MainApplication.getSpringContext() != null) {
            this.residentService = MainApplication.getSpringContext().getBean(ResidentService.class);
        } else {
            // Xử lý trường hợp context chưa sẵn sàng (ví dụ: hiển thị lỗi)
            System.err.println("SpringContext is null in ResidentController.initialize()");
            showAlert("Lỗi hệ thống", "Không thể khởi tạo dịch vụ quản lý cư dân.");
            return; // Không thể tiếp tục nếu không có service
        }

        setupTableColumns();
        loadResidentsFromDb(); // Tải dữ liệu từ DB

        currentUser = SessionContext.getInstance().getCurrentUser();    //Lấy thông tin của session
        if (currentUser != null && currentUser.getRole() == User.Role.Resident) {
            AddResident.setVisible(false);
            AddResident.setDisable(true);
            UpdateResident.setVisible(false);
            UpdateResident.setDisable(true);
            DeleteResident.setVisible(false);
            DeleteResident.setDisable(true);
        }
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        fullNameColumn.setCellValueFactory(cellData -> cellData.getValue().fullNameProperty());
        emailColumn.setCellValueFactory(cellData -> cellData.getValue().emailProperty());
        phoneColumn.setCellValueFactory(cellData -> cellData.getValue().phoneProperty());
        apartmentNumberColumn.setCellValueFactory(cellData -> cellData.getValue().apartmentNumberProperty());
        residentTable.setItems(residentsUiList);
    }

    private void loadResidentsFromDb() {
        if (residentService == null) return; // Service chưa được inject/lấy
        try {
            List<Resident> dbResidents = residentService.findAll();
            dbResidents.forEach(Resident::syncProperties); // QUAN TRỌNG: Đồng bộ properties sau khi load
            residentsUiList.setAll(dbResidents);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi tải dữ liệu", "Không thể tải danh sách cư dân từ cơ sở dữ liệu: " + e.getMessage());
        }
    }

    @FXML
    private void onSearch() {
        String keyword = searchField.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            loadResidentsFromDb(); // Tải lại toàn bộ danh sách nếu keyword rỗng
            return;
        }

        // Lọc trên danh sách đang hiển thị (đã tải từ DB)
        // Hoặc bạn có thể tạo query tìm kiếm trong ResidentService nếu muốn tìm trực tiếp từ DB
        ObservableList<Resident> filteredList = residentsUiList.stream()
                .filter(resident -> {
                    boolean matchesId = false;
                    try {
                        Long id = Long.parseLong(keyword);
                        matchesId = resident.getId() != null && resident.getId().equals(id);
                    } catch (NumberFormatException ignored) {}
                    return matchesId ||
                            (resident.getFullName() != null && resident.getFullName().toLowerCase().contains(keyword)) ||
                            (resident.getEmail() != null && resident.getEmail().toLowerCase().contains(keyword)) ||
                            (resident.getPhone() != null && resident.getPhone().toLowerCase().contains(keyword)) ||
                            (resident.getApartmentNumber() != null && resident.getApartmentNumber().toLowerCase().contains(keyword));
                })
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        residentTable.setItems(filteredList);
    }

    @FXML
    private void onAdd() {
        Resident newResident = new Resident(); // Tạo đối tượng trống cho form
        boolean saved = showResidentForm(newResident, "Thêm cư dân mới");

        if (saved && validateResident(newResident)) {
            try {
                // Không cần setId, DB sẽ tự tạo
                Resident savedResident = residentService.createResident(newResident);
                savedResident.syncProperties(); // Đồng bộ properties (bao gồm ID mới)
                residentsUiList.add(savedResident);
                residentTable.getSelectionModel().select(savedResident);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Lỗi lưu trữ", "Không thể thêm cư dân: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onUpdate() {
        Resident selected = residentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Chưa chọn cư dân", "Hãy chọn một cư dân để cập nhật.");
            return;
        }

        // Tạo bản sao để nếu người dùng hủy thì không ảnh hưởng object gốc trong list
        // Hoặc truyền trực tiếp selected và ResidentFormController sẽ sửa đổi nó.
        // Để đơn giản, truyền trực tiếp và form sẽ cập nhật các trường của 'selected'
        boolean saved = showResidentForm(selected, "Cập nhật thông tin cư dân");

        if (saved && validateResident(selected)) {
            try {
                // 'selected' đã được cập nhật bởi form
                Resident updatedResident = residentService.updateResident(selected.getId(), selected);
                updatedResident.syncProperties();

                // Cập nhật item trong list
                int index = residentsUiList.indexOf(selected); // Tìm bằng object reference
                if (index != -1) {
                    residentsUiList.set(index, updatedResident); // Thay thế bằng object đã được DB cập nhật
                } else {
                    loadResidentsFromDb(); // Fallback nếu không tìm thấy (hiếm khi xảy ra)
                }
                residentTable.refresh(); // Đảm bảo TableView cập nhật
                residentTable.getSelectionModel().select(updatedResident);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Lỗi cập nhật", "Không thể cập nhật thông tin cư dân: " + e.getMessage());
            }
        } else if (!saved && selected != null) {
            // Nếu người dùng cancel, và nếu form đã sửa đổi 'selected',
            // ta cần nạp lại từ DB để khôi phục giá trị gốc.
            // Cách tốt hơn là form làm việc trên một bản copy.
            // Hiện tại, để đơn giản, ta refresh bảng, có thể không đủ nếu 'selected' đã bị thay đổi
            residentTable.refresh();
            // Hoặc nạp lại item đó từ DB
            residentService.findById(selected.getId()).ifPresent(original -> {
                original.syncProperties();
                int index = residentsUiList.indexOf(selected);
                if (index != -1) residentsUiList.set(index, original);
            });
        }
    }

    @FXML
    private void onDelete() {
        Resident selected = residentTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Chưa chọn cư dân", "Hãy chọn một cư dân để xóa.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn có chắc muốn xóa cư dân: " + selected.getFullName() + "?",
                ButtonType.YES, ButtonType.NO);
        confirmAlert.setTitle("Xác nhận xóa");
        confirmAlert.setHeaderText(null);
        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.YES) {
            try {
                residentService.deleteResident(selected.getId());
                residentsUiList.remove(selected);
                // residentTable.setItems(residents); // Không cần nếu dùng residentsUiList và nó đã được remove
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Lỗi xóa", "Không thể xóa cư dân: " + e.getMessage());
            }
        }
    }

    // Sửa đổi showResidentForm để trả về boolean (true nếu lưu, false nếu hủy)
    private boolean showResidentForm(Resident resident, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/residentform.fxml"));
            Parent root = loader.load();
            ResidentFormController controller = loader.getController();
            controller.setResident(resident); // Truyền đối tượng resident vào form

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.initModality(Modality.APPLICATION_MODAL); // Chặn tương tác với cửa sổ khác
            stage.setScene(new Scene(root));
            // controller.setStage(stage); // Nếu form controller cần tự đóng

            stage.showAndWait(); // Chờ cho đến khi form đóng

            return controller.isSaved(); // Trả về trạng thái lưu từ form controller

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi giao diện", "Không thể mở form thông tin cư dân.");
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
        showAlert("Thông báo","Hãy chọn một cư dân để thực hiện thao tác.");
    }


    private boolean validateResident(Resident resident) {
        if (resident.getFullName() == null || resident.getFullName().trim().isEmpty()) {
            showValidationAlert("Họ tên không được để trống.");
            return false;
        }
        if (resident.getEmail() == null || !resident.getEmail().matches("^\\S+@\\S+\\.\\S+$")) {
            showValidationAlert("Email không hợp lệ.");
            return false;
        }
        if (resident.getPhone() == null || !resident.getPhone().matches("^\\d{9,11}$")) {
            showValidationAlert("Số điện thoại phải có từ 9 đến 11 chữ số.");
            return false;
        }
        if (resident.getApartmentNumber() == null || resident.getApartmentNumber().trim().isEmpty()) {
            showValidationAlert("Số căn hộ không được để trống.");
            return false;
        }
        return true;
    }

    private void showValidationAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Lỗi nhập liệu");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


}