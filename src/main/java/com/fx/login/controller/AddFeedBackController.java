package com.fx.login.controller;

import com.fx.login.model.FeedBack;
import com.fx.login.config.SessionContext;
import com.fx.login.model.User;
import com.fx.login.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AddFeedBackController {
    //Tạo giả lập User
//    private User fakeUser;
//
//    {
//        fakeUser = new User();
//        fakeUser.setFullName("Nguyen Van A");
//        fakeUser.setEmail("nguyenvana@example.com");
//    }

    @FXML
    private TextField txtTieuDe;

    @FXML
    private TextArea txtNoiDung;

    private FeedBack feedBack;

    private boolean saved = false;
    // Getter cho cờ 'saved'
    public boolean isSaved() {
        return saved;
    }

    public void setFeedBack(FeedBack feedBack) {
        this.feedBack = feedBack;
        this.saved = false; // Reset cờ khi set resident mới
        if (feedBack != null) {
            txtTieuDe.setText(feedBack.getTitle());
            txtNoiDung.setText(feedBack.getContent());
        } else {
            this.feedBack = new FeedBack(); // Đảm bảo feedBack không null
            clearFields();
        }
    }

    // Khởi tạo controller
    @FXML
    public void initialize() {
    }

    // Xử lý khi người dùng nhấn nút "Hủy"
    @FXML
    private void onCancel() {
        this.saved = false; // Đánh dấu là không lưu
        closeWindow();
    }

    // Xử lý khi người dùng nhấn nút "Gửi góp ý"
    @FXML
    private void onSubmitFeedback() {
        if (feedBack == null) {
            feedBack = new FeedBack();
        }

        //if (!validateInput()) return; // Không hợp lệ thì không làm gì



//        SessionContext.getInstance().setCurrentUser(fakeUser);

        // Lấy người dùng đang đăng nhập từ SessionContext
        User currentUser = SessionContext.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Nếu chưa đăng nhập, không cho gửi góp ý
            System.out.println("Chưa đăng nhập. Không thể gửi góp ý.");
            return;
        }

        // Cập nhật trực tiếp vào đối tượng resident đã được truyền vào
        feedBack.setTitle(txtTieuDe.getText().trim());
        feedBack.setContent(txtNoiDung.getText().trim());
        feedBack.setStatus("Chưa xử lý");
        feedBack.setSender(currentUser.getFullname());  // hoặc getEmail(), tùy
        // Không cần syncProperties() ở đây, feedBackListController sẽ làm sau khi lưu DB thành công

        this.saved = true; // Đánh dấu là đã lưu

        closeWindow();
    }

    // Kiểm tra dữ liệu đầu vào trước khi gửi
    private boolean validateInput() {
        return false;
    }

    private void clearFields() {
        txtTieuDe.clear();
        txtNoiDung.clear();
    }

    // Đóng cửa sổ hiện tại (nếu cần)
    private void closeWindow() {
        Stage stage = (Stage) txtTieuDe.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }
}