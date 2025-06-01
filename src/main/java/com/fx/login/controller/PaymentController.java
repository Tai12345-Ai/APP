package com.fx.login.controller;

import com.fx.login.model.*;
import com.fx.login.service.*;
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

@Component
@FxmlView("/ui/payment-view.fxml")
public class PaymentController implements Initializable {

    // ====================== SERVICES ======================
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentCycleService paymentCycleService;

    @Autowired
    private FeeGenerationService feeGenerationService;

    @Autowired
    private UnpaidService unpaidService;

    @Autowired
    private FeeService feeService;

    @Autowired
    private SessionService sessionService;

    // ====================== TAB 1: THANH TOÁN ======================
    @FXML private TableView<UnpaidEntity> unpaidTable;
    @FXML private TableColumn<UnpaidEntity, Long> idColumn;
    @FXML private TableColumn<UnpaidEntity, String> residentNameColumn;
    @FXML private TableColumn<UnpaidEntity, String> apartmentNameColumn;
    @FXML private TableColumn<UnpaidEntity, String> totalPaymentColumn;
    @FXML private TableColumn<UnpaidEntity, String> dueDateColumn;
    @FXML private TableColumn<UnpaidEntity, String> statusColumn;

    @FXML private TextField amountField;
    @FXML private ComboBox<String> paymentMethodComboBox;
    @FXML private Button payButton;

    // ====================== TAB 2: CHU KỲ ======================
    @FXML private TableView<PaymentCycleEntity> cycleTable;
    @FXML private TableColumn<PaymentCycleEntity, Long> cycleIdColumn;
    @FXML private TableColumn<PaymentCycleEntity, String> feeNameColumn;
    @FXML private TableColumn<PaymentCycleEntity, String> cycleTypeColumn;
    @FXML private TableColumn<PaymentCycleEntity, String> nextDueColumn;
    @FXML private TableColumn<PaymentCycleEntity, Boolean> activeColumn;

    @FXML private Button generateNowButton;

    // ====================== TAB 3: THIẾT LẬP CHU KỲ ======================
    @FXML private ComboBox<FeeEntity> feeComboBox;
    @FXML private ComboBox<String> cycleTypeComboBox;
    @FXML private DatePicker nextDueDatePicker;
    @FXML private Button createCycleButton;
    @FXML private Button createOneTimeButton;

    // Data lists
    private ObservableList<UnpaidEntity> unpaidList = FXCollections.observableArrayList();
    private ObservableList<PaymentCycleEntity> cycleList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            setupTables();
            setupComboBoxes();
            checkPermissions();
            loadData();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi khởi tạo",
                    "Không thể khởi tạo giao diện: " + e.getMessage());
        }
    }

    private void setupTables() {
        // TAB 1: Unpaid table
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        residentNameColumn.setCellValueFactory(new PropertyValueFactory<>("residentName"));
        apartmentNameColumn.setCellValueFactory(new PropertyValueFactory<>("apartmentName"));
        totalPaymentColumn.setCellValueFactory(new PropertyValueFactory<>("totalPayment"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        unpaidTable.setItems(unpaidList);

        // TAB 2: Cycle table
        cycleIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        feeNameColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getFee().getFeeName()
                )
        );
        cycleTypeColumn.setCellValueFactory(new PropertyValueFactory<>("cycleType"));
        nextDueColumn.setCellValueFactory(new PropertyValueFactory<>("nextDue"));
        activeColumn.setCellValueFactory(new PropertyValueFactory<>("active"));

        cycleTable.setItems(cycleList);
    }

    private void setupComboBoxes() {
        // Payment methods
        paymentMethodComboBox.setItems(FXCollections.observableArrayList(
                "Tiền mặt", "Chuyển khoản", "Thẻ tín dụng", "Ví điện tử"
        ));

        // Cycle types
        cycleTypeComboBox.setItems(FXCollections.observableArrayList(
                "MONTHLY", "QUARTERLY", "YEARLY"
        ));

        // Fee combo box (chỉ admin mới thấy)
        if (sessionService.isAdmin()) {
            loadFeeComboBox();
        }
    }

    private void loadFeeComboBox() {
        try {
            List<FeeEntity> fees = feeService.findAll();
            feeComboBox.setItems(FXCollections.observableArrayList(fees));

            // Custom cell factory để hiển thị tên phí
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

    private void checkPermissions() {
        boolean isAdmin = sessionService.isAdmin();

        // Admin có thể thấy tất cả, resident chỉ thấy của mình
        if (!isAdmin) {
            // Ẩn một số chức năng chỉ dành cho admin
            createCycleButton.setVisible(false);
            createOneTimeButton.setVisible(false);
            generateNowButton.setVisible(false);
            feeComboBox.setVisible(false);
            cycleTypeComboBox.setVisible(false);
            nextDueDatePicker.setVisible(false);
        }
    }

    private void loadData() {
        loadUnpaidFees();
        if (sessionService.isAdmin()) {
            loadCycles();
        }
    }

    private void loadUnpaidFees() {
        try {
            List<UnpaidEntity> fees;
            if (sessionService.isAdmin()) {
                fees = unpaidService.findAll();
            } else {
                fees = unpaidService.findMyUnpaidFees();
            }

            unpaidList.clear();
            unpaidList.addAll(fees);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải danh sách khoản phí: " + e.getMessage());
        }
    }

    private void loadCycles() {
        try {
            List<PaymentCycleEntity> cycles = paymentCycleService.findAll();
            cycleList.clear();
            cycleList.addAll(cycles);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải danh sách chu kỳ: " + e.getMessage());
        }
    }

    // ====================== EVENT HANDLERS ======================

    @FXML
    private void handlePayment() {
        UnpaidEntity selectedUnpaid = unpaidTable.getSelectionModel().getSelectedItem();
        if (selectedUnpaid == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn khoản cần thanh toán");
            return;
        }

        if (selectedUnpaid.getStatus() != null && "COMPLETED".equals(selectedUnpaid.getStatus())) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Khoản thanh toán này đã được hoàn thành");
            return;
        }

        String amountStr = amountField.getText();
        String paymentMethod = paymentMethodComboBox.getValue();

        if (amountStr == null || amountStr.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập số tiền");
            return;
        }

        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng chọn phương thức thanh toán");
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(amountStr.replace(",", "").trim());

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Số tiền phải lớn hơn 0");
                return;
            }

            BigDecimal totalPayment = new BigDecimal(selectedUnpaid.getTotalPayment().replace(",", ""));
            if (amount.compareTo(totalPayment) > 0) {
                showAlert(Alert.AlertType.WARNING, "Cảnh báo",
                        "Số tiền thanh toán không được vượt quá số tiền cần thanh toán: " + totalPayment);
                return;
            }

            PaymentEntity payment = paymentService.processPayment(selectedUnpaid.getId(), amount, paymentMethod);

            if ("COMPLETED".equals(payment.getStatus()) || "PARTIAL".equals(payment.getStatus())) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                        "Thanh toán đã được xử lý thành công!\nSố tiền: " + amount + " VNĐ");
                loadUnpaidFees();
                clearPaymentFields();
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Số tiền không hợp lệ. Vui lòng nhập số hợp lệ.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xử lý thanh toán: " + e.getMessage());
        }
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

            loadUnpaidFees();
            loadCycles();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tạo khoản thu: " + e.getMessage());
        }
    }

    @FXML
    private void handleCreateCycle() {
        FeeEntity selectedFee = feeComboBox.getValue();
        String cycleType = cycleTypeComboBox.getValue();
        LocalDate nextDue = nextDueDatePicker.getValue();

        if (selectedFee == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng chọn loại phí");
            return;
        }

        if (cycleType == null || cycleType.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng chọn chu kỳ");
            return;
        }

        if (nextDue == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng chọn ngày tạo đầu tiên");
            return;
        }

        try {
            PaymentCycleEntity newCycle = paymentCycleService.createPaymentCycle(selectedFee, cycleType, nextDue);

            showAlert(Alert.AlertType.INFORMATION, "Thành công",
                    "Đã tạo chu kỳ tự động cho phí: " + selectedFee.getFeeName());

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

        if (selectedFee == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng chọn loại phí");
            return;
        }

        if (dueDate == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng chọn ngày đến hạn");
            return;
        }

        try {
            String description = "Khoản thu đột xuất - " + selectedFee.getFeeName() + " ngày " + dueDate;
            List<UnpaidEntity> newFees = feeGenerationService.createOneTimeFees(selectedFee, dueDate, description);

            showAlert(Alert.AlertType.INFORMATION, "Thành công",
                    "Đã tạo " + newFees.size() + " khoản thu đột xuất cho phí: " + selectedFee.getFeeName());

            loadUnpaidFees();
            clearCycleFields();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tạo khoản thu đột xuất: " + e.getMessage());
        }
    }

    // ====================== HELPER METHODS ======================

    private void clearPaymentFields() {
        amountField.clear();
        paymentMethodComboBox.setValue(null);
    }

    private void clearCycleFields() {
        feeComboBox.setValue(null);
        cycleTypeComboBox.setValue(null);
        nextDueDatePicker.setValue(null);
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}