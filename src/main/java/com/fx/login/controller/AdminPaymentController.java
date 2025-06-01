package com.fx.login.controller;

import com.fx.login.model.*;
import com.fx.login.service.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Component
@FxmlView("/ui/admin-payment .fxml")
public class AdminPaymentController implements Initializable {

    @Autowired private PaymentService paymentService;
    @Autowired private PaymentCycleService paymentCycleService;
    @Autowired private FeeGenerationService feeGenerationService;
    @Autowired private UnpaidService unpaidService;
    @Autowired private FeeService feeService;
    @Autowired private SessionService sessionService;

    // TAB 1: THANH TOÁN
    @FXML private TableView<UnpaidEntity> unpaidTable;
    @FXML private TableColumn<UnpaidEntity, Long> idColumn;
    @FXML private TableColumn<UnpaidEntity, String> residentNameColumn;
    @FXML private TableColumn<UnpaidEntity, String> apartmentNameColumn;
    @FXML private TableColumn<UnpaidEntity, String> totalPaymentColumn;
    @FXML private TableColumn<UnpaidEntity, String> dueDateColumn;
    @FXML private TableColumn<UnpaidEntity, String> statusColumn;
    @FXML private TableColumn<UnpaidEntity, String> descriptionColumn;

    @FXML private TextField searchUnpaidField;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private TextField amountField;
    @FXML private ComboBox<String> paymentMethodComboBox;
    @FXML private TextField notesField;
    @FXML private Button payButton;
    @FXML private Button markPaidButton;

    // TAB 2: LỊCH SỬ THANH TOÁN
    @FXML private TableView<PaymentEntity> paymentHistoryTable;
    @FXML private TableColumn<PaymentEntity, Long> paymentIdColumn;
    @FXML private TableColumn<PaymentEntity, String> paymentDateColumn;
    @FXML private TableColumn<PaymentEntity, String> paymentAmountColumn;
    @FXML private TableColumn<PaymentEntity, String> paymentMethodColumn;
    @FXML private TableColumn<PaymentEntity, String> paymentStatusColumn;
    @FXML private TableColumn<PaymentEntity, String> paidByColumn;
    @FXML private TableColumn<PaymentEntity, String> paymentNotesColumn;

    @FXML private TextField searchHistoryField;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Button refreshHistoryButton;
    @FXML private Button exportButton;

    // TAB 3: CHU KỲ
    @FXML private TableView<PaymentCycleEntity> cycleTable;
    @FXML private TableColumn<PaymentCycleEntity, Long> cycleIdColumn;
    @FXML private TableColumn<PaymentCycleEntity, String> feeNameColumn;
    @FXML private TableColumn<PaymentCycleEntity, String> cycleTypeColumn;
    @FXML private TableColumn<PaymentCycleEntity, String> nextDueColumn;
    @FXML private TableColumn<PaymentCycleEntity, Boolean> activeColumn;
    @FXML private TableColumn<PaymentCycleEntity, String> lastGeneratedColumn;

    @FXML private Button generateNowButton;
    @FXML private Button toggleCycleButton;
    @FXML private Button deleteCycleButton;

    // TAB 4: THIẾT LẬP CHU KỲ
    @FXML private ComboBox<FeeEntity> feeComboBox;
    @FXML private ComboBox<String> cycleTypeComboBox;
    @FXML private DatePicker nextDueDatePicker;
    @FXML private TextField cycleDescriptionField;
    @FXML private Button createCycleButton;
    @FXML private Button createOneTimeButton;

    private final ObservableList<UnpaidEntity> unpaidList = FXCollections.observableArrayList();
    private final ObservableList<PaymentEntity> paymentHistoryList = FXCollections.observableArrayList();
    private final ObservableList<PaymentCycleEntity> cycleList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Kiểm tra quyền admin
        if (!sessionService.isAdmin()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi quyền truy cập",
                    "Chỉ admin mới có quyền truy cập trang quản lý thanh toán.");
            return;
        }

        setupTables();
        setupComboBoxes();
        loadData();
    }

    private void setupTables() {
        // TAB 1: Unpaid table
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        residentNameColumn.setCellValueFactory(new PropertyValueFactory<>("residentName"));
        apartmentNameColumn.setCellValueFactory(new PropertyValueFactory<>("apartmentName"));
        totalPaymentColumn.setCellValueFactory(new PropertyValueFactory<>("totalPayment"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        unpaidTable.setItems(unpaidList);

        // TAB 2: Payment history table
        paymentIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        paymentDateColumn.setCellValueFactory(new PropertyValueFactory<>("paymentDate"));
        paymentAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        paymentMethodColumn.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        paymentStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        paidByColumn.setCellValueFactory(new PropertyValueFactory<>("paidBy"));
        paymentNotesColumn.setCellValueFactory(new PropertyValueFactory<>("notes"));
        paymentHistoryTable.setItems(paymentHistoryList);

        // TAB 3: Cycle table
        cycleIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        feeNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFee().getFeeName())
        );
        cycleTypeColumn.setCellValueFactory(new PropertyValueFactory<>("cycleType"));
        nextDueColumn.setCellValueFactory(new PropertyValueFactory<>("nextDue"));
        activeColumn.setCellValueFactory(new PropertyValueFactory<>("active"));
        lastGeneratedColumn.setCellValueFactory(new PropertyValueFactory<>("lastGenerated"));
        cycleTable.setItems(cycleList);
    }

    private void setupComboBoxes() {
        // Status filter
        statusFilterComboBox.setItems(FXCollections.observableArrayList(
                "Tất cả", "PENDING", "COMPLETED", "OVERDUE"
        ));
        statusFilterComboBox.setValue("Tất cả");

        // Payment methods
        paymentMethodComboBox.setItems(FXCollections.observableArrayList(
                "Tiền mặt", "Chuyển khoản", "Thẻ tín dụng", "Ví điện tử"
        ));

        // Cycle types
        cycleTypeComboBox.setItems(FXCollections.observableArrayList(
                "MONTHLY", "QUARTERLY", "YEARLY"
        ));

        // Fee combo box
        loadFeeComboBox();
    }

    private void loadFeeComboBox() {
        try {
            List<FeeEntity> fees = feeService.findAll();
            feeComboBox.setItems(FXCollections.observableArrayList(fees));

            feeComboBox.setCellFactory(listView -> new ListCell<FeeEntity>() {
                @Override
                protected void updateItem(FeeEntity fee, boolean empty) {
                    super.updateItem(fee, empty);
                    setText(empty || fee == null ? null : fee.getFeeName());
                }
            });

            feeComboBox.setButtonCell(new ListCell<FeeEntity>() {
                @Override
                protected void updateItem(FeeEntity fee, boolean empty) {
                    super.updateItem(fee, empty);
                    setText(empty || fee == null ? null : fee.getFeeName());
                }
            });
        } catch (Exception e) {
            System.err.println("Lỗi load fee combo box: " + e.getMessage());
        }
    }

    private void loadData() {
        loadUnpaidFees();
        loadPaymentHistory();
        loadCycles();
    }

    private void loadUnpaidFees() {
        try {
            List<UnpaidEntity> fees = unpaidService.findAll();
            unpaidList.setAll(fees);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải danh sách khoản phí: " + e.getMessage());
        }
    }

    private void loadPaymentHistory() {
        try {
            List<PaymentEntity> payments = paymentService.findAll();
            paymentHistoryList.setAll(payments);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải lịch sử thanh toán: " + e.getMessage());
        }
    }

    private void loadCycles() {
        try {
            List<PaymentCycleEntity> cycles = paymentCycleService.findAll();
            cycleList.setAll(cycles);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải danh sách chu kỳ: " + e.getMessage());
        }
    }

    // EVENT HANDLERS

    @FXML
    private void searchUnpaid() {
        String keyword = searchUnpaidField.getText().trim().toLowerCase();
        String statusFilter = statusFilterComboBox.getValue();

        try {
            List<UnpaidEntity> allFees = unpaidService.findAll();
            List<UnpaidEntity> filteredFees = allFees.stream()
                    .filter(fee -> {
                        boolean matchesKeyword = keyword.isEmpty() ||
                                (fee.getResidentName() != null && fee.getResidentName().toLowerCase().contains(keyword)) ||
                                (fee.getApartmentName() != null && fee.getApartmentName().toLowerCase().contains(keyword));

                        boolean matchesStatus = "Tất cả".equals(statusFilter) ||
                                statusFilter.equals(fee.getStatus());

                        return matchesKeyword && matchesStatus;
                    })
                    .collect(Collectors.toList());

            unpaidList.setAll(filteredFees);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tìm kiếm: " + e.getMessage());
        }
    }

    @FXML
    private void handlePayment() {
        UnpaidEntity selectedUnpaid = unpaidTable.getSelectionModel().getSelectedItem();
        if (selectedUnpaid == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn khoản cần thanh toán");
            return;
        }

        String amountStr = amountField.getText();
        String paymentMethod = paymentMethodComboBox.getValue();
        String notes = notesField.getText();

        if (amountStr == null || amountStr.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập số tiền");
            return;
        }

        if (paymentMethod == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng chọn phương thức thanh toán");
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(amountStr.replace(",", "").trim());
            PaymentEntity payment = paymentService.processPayment(selectedUnpaid.getId(), amount, paymentMethod);
            payment.setNotes(notes);

            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Thanh toán đã được xử lý thành công!");
            loadData();
            clearPaymentFields();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xử lý thanh toán: " + e.getMessage());
        }
    }

    @FXML
    private void markAsPaid() {
        UnpaidEntity selectedUnpaid = unpaidTable.getSelectionModel().getSelectedItem();
        if (selectedUnpaid == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn khoản cần đánh dấu");
            return;
        }

        try {
            selectedUnpaid.setStatus("COMPLETED");
            unpaidService.save(selectedUnpaid);

            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã đánh dấu khoản phí là đã thanh toán");
            loadUnpaidFees();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật trạng thái: " + e.getMessage());
        }
    }

    @FXML
    private void searchHistory() {
        String keyword = searchHistoryField.getText().trim().toLowerCase();
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();

        try {
            List<PaymentEntity> allPayments = paymentService.findAll();
            List<PaymentEntity> filteredPayments = allPayments.stream()
                    .filter(payment -> {
                        boolean matchesKeyword = keyword.isEmpty() ||
                                (payment.getPaidBy() != null && payment.getPaidBy().toLowerCase().contains(keyword)) ||
                                (payment.getPaymentMethod() != null && payment.getPaymentMethod().toLowerCase().contains(keyword));

                        boolean matchesDateRange = true;
                        if (fromDate != null && payment.getPaymentDate() != null) {
                            matchesDateRange = !payment.getPaymentDate().toLocalDate().isBefore(fromDate);
                        }
                        if (toDate != null && payment.getPaymentDate() != null && matchesDateRange) {
                            matchesDateRange = !payment.getPaymentDate().toLocalDate().isAfter(toDate);
                        }

                        return matchesKeyword && matchesDateRange;
                    })
                    .collect(Collectors.toList());

            paymentHistoryList.setAll(filteredPayments);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tìm kiếm lịch sử: " + e.getMessage());
        }
    }

    @FXML
    private void refreshHistory() {
        loadPaymentHistory();
        searchHistoryField.clear();
        fromDatePicker.setValue(null);
        toDatePicker.setValue(null);
    }

    @FXML
    private void exportReport() {
        // TODO: Implement export functionality
        showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Chức năng xuất báo cáo đang được phát triển");
    }

    @FXML
    private void handleGenerateNow() {
        PaymentCycleEntity selectedCycle = cycleTable.getSelectionModel().getSelectedItem();
        if (selectedCycle == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn chu kỳ cần tạo khoản thu");
            return;
        }

        try {
            List<UnpaidEntity> newFees = feeGenerationService.generateFeesForCycle(selectedCycle);
            showAlert(Alert.AlertType.INFORMATION, "Thành công",
                    "Đã tạo " + newFees.size() + " khoản thu mới cho chu kỳ: " +
                            selectedCycle.getFee().getFeeName());
            loadData();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tạo khoản thu: " + e.getMessage());
        }
    }

    @FXML
    private void toggleCycle() {
        PaymentCycleEntity selectedCycle = cycleTable.getSelectionModel().getSelectedItem();
        if (selectedCycle == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn chu kỳ");
            return;
        }

        try {
            selectedCycle.setActive(!selectedCycle.isActive()); // Sửa: dùng isActive() thay vì getActive()
            paymentCycleService.save(selectedCycle);
            loadCycles();
            showAlert(Alert.AlertType.INFORMATION, "Thành công",
                    "Đã " + (selectedCycle.isActive() ? "bật" : "tắt") + " chu kỳ"); // Sửa: dùng isActive()
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể thay đổi trạng thái chu kỳ: " + e.getMessage());
        }
    }

    @FXML
    private void deleteCycle() {
        PaymentCycleEntity selectedCycle = cycleTable.getSelectionModel().getSelectedItem();
        if (selectedCycle == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn chu kỳ cần xóa");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn có chắc muốn xóa chu kỳ này?", ButtonType.YES, ButtonType.NO);
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    paymentCycleService.delete(selectedCycle.getId());
                    loadCycles();
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã xóa chu kỳ");
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xóa chu kỳ: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleCreateCycle() {
        FeeEntity selectedFee = feeComboBox.getValue();
        String cycleType = cycleTypeComboBox.getValue();
        LocalDate nextDue = nextDueDatePicker.getValue();

        if (selectedFee == null || cycleType == null || nextDue == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng điền đầy đủ thông tin");
            return;
        }

        try {
            PaymentCycleEntity newCycle = paymentCycleService.createPaymentCycle(selectedFee, cycleType, nextDue);
            String description = cycleDescriptionField.getText();
            if (description != null && !description.trim().isEmpty()) {
                // Nếu có field description trong entity
                // newCycle.setDescription(description);
                // paymentCycleService.save(newCycle);
            }

            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã tạo chu kỳ tự động");
            loadCycles();
            clearCycleFields();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tạo chu kỳ: " + e.getMessage());
        }
    }

    @FXML
    private void handleCreateOneTime() {
        FeeEntity selectedFee = feeComboBox.getValue();
        LocalDate dueDate = nextDueDatePicker.getValue();

        if (selectedFee == null || dueDate == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng chọn loại phí và ngày đến hạn");
            return;
        }

        try {
            String description = "Khoản thu đột xuất - " + selectedFee.getFeeName();
            List<UnpaidEntity> newFees = feeGenerationService.createOneTimeFees(selectedFee, dueDate, description);

            showAlert(Alert.AlertType.INFORMATION, "Thành công",
                    "Đã tạo " + newFees.size() + " khoản thu đột xuất");
            loadUnpaidFees();
            clearCycleFields();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tạo khoản thu: " + e.getMessage());
        }
    }

    private void clearPaymentFields() {
        amountField.clear();
        paymentMethodComboBox.setValue(null);
        notesField.clear();
    }

    private void clearCycleFields() {
        feeComboBox.setValue(null);
        cycleTypeComboBox.setValue(null);
        nextDueDatePicker.setValue(null);
        cycleDescriptionField.clear();
    }

    // Thêm method showAlert để sửa lỗi
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}