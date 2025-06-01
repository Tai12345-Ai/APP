package com.fx.login.controller;

import com.fx.login.model.UnpaidEntity;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

public class UnpaidFormController {
    @FXML
    private TextField residentName;
    @FXML
    private TextField apartmentName;
    @FXML
    private TextField totalPayment;
    @FXML
    private TextField dueDate;

    private String feeID;

    private UnpaidEntity unpaid;
    private boolean saved = false; // Cờ để biết form có được lưu không
    private boolean isUpdateMode = false; // Cờ để biết có cần tắt editable trong hàm update không

    public void setIsUpdateMode(boolean updateMode) {
        this.isUpdateMode = updateMode;
    }

    public void setUnpaid(UnpaidEntity unpaid) {
        this.unpaid = unpaid;
        this.saved = false; // Reset cờ khi set resident mới
        if (unpaid != null) {
            residentName.setText(unpaid.getResidentName());
            apartmentName.setText(unpaid.getApartmentName());
            totalPayment.setText(unpaid.getTotalPayment());

            if(!isUpdateMode) totalPayment.setEditable(false);
            if(totalPayment.getText().equals("Bấm \"Xem\" để biết chính xác")) totalPayment.setText("");
            dueDate.setText(unpaid.getDueDate());
            feeID = String.valueOf(unpaid.getFeeID());
        } else {
            this.unpaid = new UnpaidEntity(); // Đảm bảo unpaid không null
            clearFields();
        }
    }

    // Getter cho cờ 'saved'
    public boolean isSaved() {
        return saved;
    }

    private void showValidationAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Lỗi nhập liệu");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean validateUnpaid() {
        // Kiểm tra tên chủ hộ
        if (residentName.getText() == null || residentName.getText().trim().isEmpty()) {
            showValidationAlert("Tên chủ hộ không được để trống.");
            return false;
        }

        // Kiểm tra số căn hộ
        if (apartmentName.getText() == null || apartmentName.getText().trim().isEmpty()) {
            showValidationAlert("Số căn hộ không được để trống.");
            return false;
        } else {
            String apartment = apartmentName.getText().trim();
            if (!apartment.matches("^[AB]\\d{3}$")) {
                showValidationAlert("Số căn hộ phải có định dạng từ A000 đến A999 hoặc B000 đến B999.");
                return false;
            }
        }

        // Kiểm tra số tiền còn thiếu
        if (totalPayment.getText() == null || totalPayment.getText().trim().isEmpty()) {
            showValidationAlert("Số tiền cần nộp không được để trống.");
            return false;
        } else {
            String amountText = totalPayment.getText().trim().replace(" VNĐ", "").replace(",", "");
            try {
                double amount = Double.parseDouble(amountText);
                if (amount < 1000 || amount > 10000000) {
                    showValidationAlert("Số tiền cần nộp phải từ 1,000 đến 10,000,000 VNĐ.");
                    return false;
                }
                // Tự động thêm " VNĐ" nếu chưa có
                if (!totalPayment.getText().trim().endsWith("VNĐ")) {
                    totalPayment.setText(String.format("%,.0f VNĐ", amount));
                }
            } catch (NumberFormatException e) {
                showValidationAlert("Số tiền cần nộp phải là một số hợp lệ.");
                return false;
            }
        }

        // Kiểm tra hạn nộp
        if (dueDate.getText() == null || dueDate.getText().trim().isEmpty()) {
            showValidationAlert("Hạn nộp không được để trống.");
            return false;
        } else {
            String dateInput = dueDate.getText().trim();

            // Kiểm tra định dạng chung bằng regex để đảm bảo đúng cấu trúc và năm 4 chữ số
            if (!dateInput.matches("^\\d{1,2}/\\d{1,2}/\\d{4}$")) {
                showValidationAlert("Hạn nộp phải có định dạng DD/MM/yyyy (ví dụ: 05/09/2025).");
                return false;
            }

            DateTimeFormatter inputFormatter = new DateTimeFormatterBuilder()
                    .appendValue(ChronoField.DAY_OF_MONTH)
                    .appendLiteral('/')
                    .appendValue(ChronoField.MONTH_OF_YEAR)
                    .appendLiteral('/')
                    .appendValue(ChronoField.YEAR, 4)
                    .toFormatter();

            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            try {
                LocalDate parsedDate = LocalDate.parse(dateInput, inputFormatter);

                if (parsedDate.getYear() < 2000) {
                    showValidationAlert("Năm phải lớn hơn hoặc bằng 2000.");
                    return false;
                }

                dueDate.setText(parsedDate.format(outputFormatter));
            } catch (DateTimeParseException e) {
                showValidationAlert("Hạn nộp không hợp lệ. Vui lòng nhập đúng định dạng DD/MM/yyyy.");
                return false;
            }
        }

        return true;
    }

    @FXML
    private void onSave() {
        if (unpaid == null) { // Trường hợp này không nên xảy ra nếu setResident được gọi đúng
            unpaid = new UnpaidEntity();
        }

        if (!validateUnpaid()) return; // Không hợp lệ thì không làm gì

        // Cập nhật trực tiếp vào đối tượng resident đã được truyền vào
        unpaid.setResidentName(residentName.getText().trim());
        unpaid.setApartmentName(apartmentName.getText().trim());
        unpaid.setTotalPayment(totalPayment.getText().trim());
        unpaid.setDueDate(dueDate.getText().trim());
        unpaid.setFeeID(Long.valueOf(feeID.trim()));
        // Không cần syncProperties() ở đây, UnpaidListController sẽ làm sau khi lưu DB thành công

        this.saved = true; // Đánh dấu là đã lưu
        closeWindow();
    }

    // Thêm phương thức này nếu có nút "Hủy" (Cancel) trong FXML
    @FXML
    private void onCancel() {
        this.saved = false; // Đánh dấu là không lưu
        closeWindow();
    }


    private void clearFields() {
        residentName.clear();
        apartmentName.clear();
        totalPayment.clear();
        dueDate.clear();
    }

    private void closeWindow() {
        Stage stage = (Stage) residentName.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }
}
