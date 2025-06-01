package com.fx.login.controller;

import com.fx.login.model.FeedBack;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;

public class DetailFeedBackController {

    @FXML
    private TextField txtTieuDe;

    @FXML
    private TextArea txtNoiDung;

    @FXML
    private TextField txtThoiGianGui;

    @FXML
    private TextField txtNguoiGui;

    @FXML
    private TextField txtTrangThai;

    private FeedBack feedBack;
    private boolean saved = false;
    // Getter cho cờ 'saved'
    public boolean isSaved() {
        return saved;
    }

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    public void setFeedBack(FeedBack feedBack) {
        this.feedBack = feedBack;
        this.saved = false; // Reset cờ khi set resident mới
        if (feedBack != null) {
            txtTieuDe.setText(feedBack.getTitle());
            txtNoiDung.setText(feedBack.getContent());
            txtThoiGianGui.setText(formatter.format(feedBack.getCreatedAt()));
            txtNguoiGui.setText(feedBack.getSender());
            txtTrangThai.setText(feedBack.getStatus());
            txtTieuDe.setEditable(false);
            txtNoiDung.setEditable(false);
            txtThoiGianGui.setEditable(false);
            txtNguoiGui.setEditable(false);
            txtTrangThai.setEditable(false);
        } else {
            this.feedBack = new FeedBack(); // Đảm bảo feedBack không null
            clearFields();
        }
    }

    // Khởi tạo controller
    @FXML
    public void initialize() {
    }

    private void clearFields() {
        txtTieuDe.clear();
        txtNoiDung.clear();
        txtThoiGianGui.clear();
        txtNguoiGui.clear();
        txtTrangThai.clear();
    }

    // Đóng cửa sổ hiện tại (nếu cần)
    private void closeWindow() {
        Stage stage = (Stage) txtTieuDe.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }
}
