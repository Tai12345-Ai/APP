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
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Component
@FxmlView("/ui/resident-payment .fxml")
public class ResidentPaymentController implements Initializable {

    @Autowired private PaymentService paymentService;
    @Autowired private UnpaidService unpaidService;
    @Autowired private SessionService sessionService;

    // TAB 1: KHOẢN CHƯA THANH TOÁN
    @FXML private TableView<UnpaidEntity> unpaidTable;
    @FXML private TableColumn<UnpaidEntity, Long> idColumn;
    @FXML private TableColumn<UnpaidEntity, String> feeNameColumn;
    @FXML private TableColumn<UnpaidEntity, String> totalPaymentColumn;
    @FXML private TableColumn<UnpaidEntity, String> dueDateColumn;
    @FXML private TableColumn<UnpaidEntity, String> statusColumn;
    @FXML private TableColumn<UnpaidEntity, String> descriptionColumn;

    @FXML private TextField searchField;
    @FXML private Label selectedFeeLabel;
    @FXML private Label amountLabel;
    @FXML private ComboBox<String> paymentMethodComboBox;
    @FXML private TextField notesField;
    @FXML private Button payButton;

    // TAB 2: LỊCH SỬ THANH TOÁN
    @FXML private TableView<PaymentEntity> paymentHistoryTable;
    @FXML private TableColumn<PaymentEntity, Long> paymentIdColumn;
    @FXML private TableColumn<PaymentEntity, String> paymentDateColumn;
    @FXML private TableColumn<PaymentEntity, String> paymentFeeNameColumn;
    @FXML private TableColumn<PaymentEntity, String> paymentAmountColumn;
    @FXML private TableColumn<PaymentEntity, String> paymentMethodColumn;
    @FXML private TableColumn<PaymentEntity, String> paymentStatusColumn;
    @FXML private TableColumn<PaymentEntity, String> paymentNotesColumn;

    @FXML private TextField searchHistoryField;
    @FXML private Button refreshHistoryButton;

    // TAB 3: THỐNG KÊ
    @FXML private Label totalFeesCountLabel;
    @FXML private Label unpaidCountLabel;
    @FXML private Label paidCountLabel;
    @FXML private Label totalPaidAmountLabel;

    // Labels
    @FXML private Label residentInfoLabel;

    private final ObservableList<UnpaidEntity> unpaidList = FXCollections.observableArrayList();
    private final ObservableList<PaymentEntity> paymentHistoryList = FXCollections.observableArrayList();
    private UnpaidEntity selectedUnpaid = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Kiểm tra quyền resident
        if (!sessionService.isResident()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi quyền truy cập",
                    "Chỉ cư dân mới có quyền truy cập trang thanh toán.");
            return;
        }

        setupUserInfo();
        setupTables();
        setupComboBoxes();
        loadData();
        updateStatistics();
    }

    private void setupUserInfo() {
        User currentUser = sessionService.getCurrentUser();
        if (currentUser != null) {
            residentInfoLabel.setText("Cư dân: " + currentUser.getResidentFullName() +
                    " - Căn hộ: " + currentUser.getApartmentNumber());
        }
    }

    private void setupTables() {
        // TAB 1: Unpaid table
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        feeNameColumn.setCellValueFactory(cellData -> {
            // Lấy tên phí từ FeeID hoặc description
            String feeName = cellData.getValue().getDescription();
            if (feeName == null || feeName.isEmpty()) {
                feeName = "Phí ID: " + cellData.getValue().getFeeID();
            }
            return new SimpleStringProperty(feeName);
        });
        totalPaymentColumn.setCellValueFactory(new PropertyValueFactory<>("totalPayment"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        unpaidTable.setItems(unpaidList);

        // Thêm listener cho selection
        unpaidTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedUnpaid = newSelection;
                updatePaymentInfo(newSelection);
            }
        });

        // TAB 2: Payment history table
        paymentIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        paymentDateColumn.setCellValueFactory(new PropertyValueFactory<>("paymentDate"));
        paymentFeeNameColumn.setCellValueFactory(cellData -> {
            String feeName = "Không xác định";
            try {
                // Sửa lỗi: dùng getUnpaid().getId() thay vì getUnpaidId()
                if (cellData.getValue().getUnpaid() != null) {
                    // Tìm unpaid entity để lấy tên phí
                    UnpaidEntity unpaid = cellData.getValue().getUnpaid();
                    if (unpaid != null && unpaid.getDescription() != null) {
                        feeName = unpaid.getDescription();
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
            return new SimpleStringProperty(feeName);
        });
        paymentAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        paymentMethodColumn.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        paymentStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        paymentNotesColumn.setCellValueFactory(new PropertyValueFactory<>("notes"));
        paymentHistoryTable.setItems(paymentHistoryList);
    }

    private void setupComboBoxes() {
        paymentMethodComboBox.setItems(FXCollections.observableArrayList(
                "Tiền mặt", "Chuyển khoản", "Thẻ tín dụng", "Ví điện tử"
        ));
    }

    private void loadData() {
        loadMyUnpaidFees();
        loadMyPaymentHistory();
    }

    private void loadMyUnpaidFees() {
        try {
            List<UnpaidEntity> myFees = unpaidService.findMyUnpaidFees();
            // Chỉ hiển thị những khoản chưa thanh toán
            List<UnpaidEntity> pendingFees = myFees.stream()
                    .filter(fee -> "PENDING".equals(fee.getStatus()))
                    .collect(Collectors.toList());
            unpaidList.setAll(pendingFees);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải danh sách khoản phí: " + e.getMessage());
        }
    }

    private void loadMyPaymentHistory() {
        try {
            // Lấy lịch sử thanh toán của resident hiện tại
            List<PaymentEntity> myPayments = paymentService.findMyPayments();
            paymentHistoryList.setAll(myPayments);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải lịch sử thanh toán: " + e.getMessage());
        }
    }

    private void updatePaymentInfo(UnpaidEntity unpaid) {
        if (unpaid != null) {
            selectedFeeLabel.setText(unpaid.getDescription() != null ?
                    unpaid.getDescription() : "Phí ID: " + unpaid.getFeeID());
            amountLabel.setText(formatCurrency(unpaid.getTotalPayment()) + " VNĐ");
            amountLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #F44336;");
        } else {
            selectedFeeLabel.setText("Chưa chọn");
            amountLabel.setText("0 VNĐ");
        }
    }

    private void updateStatistics() {
        try {
            List<UnpaidEntity> allMyFees = unpaidService.findMyUnpaidFees();
            List<PaymentEntity> myPayments = paymentService.findMyPayments();

            int totalFees = allMyFees.size();
            int unpaidCount = (int) allMyFees.stream()
                    .filter(fee -> "PENDING".equals(fee.getStatus()))
                    .count();
            int paidCount = totalFees - unpaidCount;

            // Sửa lỗi: BigDecimal không có method toLowerCase() và replace()
            BigDecimal totalPaidAmount = myPayments.stream()
                    .filter(payment -> "COMPLETED".equals(payment.getStatus()))
                    .map(PaymentEntity::getAmount) // Lấy BigDecimal trực tiếp
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalFeesCountLabel.setText(String.valueOf(totalFees));
            unpaidCountLabel.setText(String.valueOf(unpaidCount));
            paidCountLabel.setText(String.valueOf(paidCount));
            totalPaidAmountLabel.setText(formatCurrency(totalPaidAmount.toString()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // EVENT HANDLERS

    @FXML
    private void searchMyUnpaid() {
        String keyword = searchField.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            loadMyUnpaidFees();
            return;
        }

        try {
            List<UnpaidEntity> allMyFees = unpaidService.findMyUnpaidFees();
            List<UnpaidEntity> filteredFees = allMyFees.stream()
                    .filter(fee -> "PENDING".equals(fee.getStatus()) && (
                            (fee.getDescription() != null && fee.getDescription().toLowerCase().contains(keyword)) ||
                                    (fee.getTotalPayment() != null && fee.getTotalPayment().toLowerCase().contains(keyword)) ||
                                    fee.getFeeID().toString().contains(keyword)
                    ))
                    .collect(Collectors.toList());

            unpaidList.setAll(filteredFees);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tìm kiếm: " + e.getMessage());
        }
    }

    @FXML
    private void handlePayment() {
        if (selectedUnpaid == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn khoản phí cần thanh toán");
            return;
        }

        String paymentMethod = paymentMethodComboBox.getValue();
        if (paymentMethod == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng chọn phương thức thanh toán");
            return;
        }

        try {
            // Lấy số tiền từ unpaid entity
            String amountStr = selectedUnpaid.getTotalPayment().replace(",", "").replace(" VNĐ", "");
            BigDecimal amount = new BigDecimal(amountStr);

            PaymentEntity payment = paymentService.processPayment(selectedUnpaid.getId(), amount, paymentMethod);
            if (notesField.getText() != null && !notesField.getText().trim().isEmpty()) {
                payment.setNotes(notesField.getText().trim());
                paymentService.save(payment);
            }

            showAlert(Alert.AlertType.INFORMATION, "Thành công",
                    "Thanh toán đã được xử lý thành công!\nSố tiền: " + formatCurrency(amount.toString()) + " VNĐ");

            loadData();
            updateStatistics();
            clearPaymentFields();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xử lý thanh toán: " + e.getMessage());
        }
    }

    @FXML
    private void searchHistory() {
        String keyword = searchHistoryField.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            loadMyPaymentHistory();
            return;
        }

        try {
            List<PaymentEntity> allMyPayments = paymentService.findMyPayments();
            List<PaymentEntity> filteredPayments = allMyPayments.stream()
                    .filter(payment ->
                            (payment.getPaymentMethod() != null && payment.getPaymentMethod().toLowerCase().contains(keyword)) ||
                                    // Sửa lỗi: BigDecimal không có toLowerCase()
                                    (payment.getAmount() != null && payment.getAmount().toString().toLowerCase().contains(keyword)) ||
                                    (payment.getNotes() != null && payment.getNotes().toLowerCase().contains(keyword))
                    )
                    .collect(Collectors.toList());

            paymentHistoryList.setAll(filteredPayments);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tìm kiếm lịch sử: " + e.getMessage());
        }
    }

    @FXML
    private void refreshHistory() {
        loadMyPaymentHistory();
        searchHistoryField.clear();
    }

    @FXML
    private void refreshData() {
        loadData();
        updateStatistics();
        searchField.clear();
    }

    @FXML
    private void exportReceipt() {
        PaymentEntity selectedPayment = paymentHistoryTable.getSelectionModel().getSelectedItem();
        if (selectedPayment == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn giao dịch cần xuất hóa đơn");
            return;
        }

        // TODO: Implement receipt export
        showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Chức năng xuất hóa đơn đang được phát triển");
    }

    private void clearPaymentFields() {
        paymentMethodComboBox.setValue(null);
        notesField.clear();
        selectedUnpaid = null;
        selectedFeeLabel.setText("Chưa chọn");
        amountLabel.setText("0 VNĐ");
        unpaidTable.getSelectionModel().clearSelection();
    }

    private String formatCurrency(String amount) {
        try {
            String cleanAmount = amount.replace(",", "").replace(" VNĐ", "");
            double value = Double.parseDouble(cleanAmount);
            NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
            return formatter.format(value);
        } catch (Exception e) {
            return amount;
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}