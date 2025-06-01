package com.fx.login.controller;

import com.fx.login.model.Resident;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ResidentFormController {

    @FXML
    private TextField fullNameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField apartmentNumberField;

    // @FXML private Button cancelButton; // Nếu bạn có nút Cancel

    private Resident resident;
    private boolean saved = false; // Cờ để biết form có được lưu không

    public void setResident(Resident resident) {
        this.resident = resident;
        this.saved = false; // Reset cờ khi set resident mới
        if (resident != null) {
            fullNameField.setText(resident.getFullName());
            emailField.setText(resident.getEmail());
            phoneField.setText(resident.getPhone());
            apartmentNumberField.setText(resident.getApartmentNumber());
        } else {
            // Nếu là thêm mới, resident có thể là new ResidentEntity() rỗng
            // hoặc bạn có thể khởi tạo một resident mới ở đây nếu cần
            this.resident = new Resident(); // Đảm bảo resident không null
            clearFields();
        }
    }

    // Getter cho cờ 'saved'
    public boolean isSaved() {
        return saved;
    }

    // Getter cho resident (nếu ResidentController cần lấy lại)
    // public ResidentEntity getResident() { return resident; }

    @FXML
    private void onSave() {
        if (resident == null) { // Trường hợp này không nên xảy ra nếu setResident được gọi đúng
            resident = new Resident();
        }
        // Cập nhật trực tiếp vào đối tượng resident đã được truyền vào
        resident.setFullName(fullNameField.getText());
        resident.setEmail(emailField.getText());
        resident.setPhone(phoneField.getText());
        resident.setApartmentNumber(apartmentNumberField.getText());
        // Không cần syncProperties() ở đây, ResidentController sẽ làm sau khi lưu DB thành công

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
        fullNameField.clear();
        emailField.clear();
        phoneField.clear();
        apartmentNumberField.clear();
    }

    private void closeWindow() {
        Stage stage = (Stage) fullNameField.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }
}