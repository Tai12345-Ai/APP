package com.fx.login.controller;

import com.fx.login.config.SessionContext;
import com.fx.login.model.FeeEntity;
import com.fx.login.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class FeeFormController {

    @FXML
    private TextField feeName;
    @FXML
    private TextField amountDue;
    @FXML
    private TextField monthlyFee;

    private FeeEntity fee;
    private boolean saved = false; // Cờ để biết form có được lưu không

    public void setFee(FeeEntity fee) {
        this.fee = fee;
        this.saved = false; // Reset cờ khi set resident mới
        if (fee != null) {
            feeName.setText(fee.getFeeName());
            amountDue.setText(fee.getAmountDue());
            monthlyFee.setText(fee.getMonthlyFee());
        } else {
            this.fee = new FeeEntity(); // Đảm bảo resident không null
            clearFields();
            amountDue.setText("không cố định");
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

    private boolean validateFee() {
        // Tên khoản thu
        if (feeName.getText() == null || feeName.getText().trim().isEmpty()) {
            showValidationAlert("Tên khoản thu không được để trống.");
            return false;
        }

        // Số tiền cần nộp (từ 1000 đến 10,000,000 VNĐ)
        String amountTextRaw = amountDue.getText();
        if (amountTextRaw == null || amountTextRaw.trim().isEmpty()) {
            amountDue.setText("Bấm \"Xem\" để biết chính xác");
        } else {
            String amountText = amountTextRaw.trim();
            if (amountText.equalsIgnoreCase("Bấm \"Xem\" để biết chính xác")) {
                // Cho phép
            } else {
                amountText = amountText.replace(" VNĐ", "").replace(",", "");
                try {
                    double amount = Double.parseDouble(amountText);
                    if (amount < 1000 || amount > 10000000) {
                        showValidationAlert("Số tiền cần nộp phải từ 1,000 đến 10,000,000 VNĐ hoặc để trống.");
                        return false;
                    }
                    // Tự động thêm " VNĐ"
                    amountDue.setText(String.format("%,.0f VNĐ", amount));
                } catch (NumberFormatException e) {
                    showValidationAlert("Số tiền cần nộp phải là số hợp lệ hoặc để trống.");
                    return false;
                }
            }
        }

        // Khoản thu của tháng (định dạng MM/yyyy)
        if (monthlyFee.getText() == null || monthlyFee.getText().trim().isEmpty()) {
            showValidationAlert("Khoản thu của tháng không được để trống.");
            return false;
        } else {
            String monthInput = monthlyFee.getText().trim();
            if (!monthInput.matches("^(\\d{1,2})/\\d{4}$")) {
                showValidationAlert("Khoản thu của tháng phải có định dạng M/yyyy hoặc MM/yyyy (ví dụ: 3/2025 hoặc 03/2025).");
                return false;
            }

            // Tách tháng và năm, chuẩn hóa về MM/yyyy
            String[] parts = monthInput.split("/");
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]);

            if (month < 1 || month > 12) {
                showValidationAlert("Tháng phải nằm trong khoảng từ 1 đến 12.");
                return false;
            }

            if (year < 2000) {
                showValidationAlert("Năm phải lớn hơn hoặc bằng 2000.");
                return false;
            }

            String formattedMonth = String.format("%02d/%d", month, year);
            monthlyFee.setText(formattedMonth);
        }
        return true;
    }

    @FXML
    private void onSave() {
        if (fee == null) { // Trường hợp này không nên xảy ra nếu setFee được gọi đúng
            fee = new FeeEntity();
        }

        if (!validateFee()) return; // Không hợp lệ thì không làm gì

        // Cập nhật trực tiếp vào đối tượng fee đã được truyền vào
        fee.setFeeName(feeName.getText().trim());
        fee.setAmountDue(amountDue.getText().trim());
        fee.setMonthlyFee(monthlyFee.getText().trim());
        // Không cần syncProperties() ở đây, FeeController sẽ làm sau khi lưu DB thành công

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
        feeName.clear();
        amountDue.clear();
        monthlyFee.clear();
    }

    private void closeWindow() {
        Stage stage = (Stage) feeName.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }


}