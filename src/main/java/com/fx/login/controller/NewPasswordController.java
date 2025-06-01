package com.fx.login.controller;

import com.fx.login.config.Router;
import com.fx.login.config.SessionContext;
import com.fx.login.model.User;
import com.fx.login.service.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@FxmlView("/ui/newpassword.fxml")
public class NewPasswordController {
    @FXML
    private PasswordField passMoiField;

    @FXML
    private PasswordField passMoiRetypeField;

    @FXML
    private Label lblChangePassword;
    @Autowired private UserService userService;
    @Autowired private Router router;
    User currentUser;
    @FXML
    public void initialize() {
        currentUser = SessionContext.getInstance().getCurrentUser();    //Lấy thông tin của session
    }

    public void saveUser(ActionEvent e){
        if (!(passMoiField.getText().equals(passMoiRetypeField.getText()))) lblChangePassword.setText("Mật khẩu nhập lại không trùng khớp!");
        else if (passMoiField.getText().equals(currentUser.getPassword())) lblChangePassword.setText("Đây là mật khẩu cũ của bạn!");
        else {
            currentUser.setPassword(passMoiField.getText());
            userService.save(currentUser);
            lblChangePassword.setText("Thay đổi mật khẩu thành công");
            lblChangePassword.setTextFill(Color.web("#229abb"));
        }
    }

    public void loadLogin(MouseEvent mouseEvent) {
        router.navigateMouseEvent(LoginController.class, mouseEvent);
    }
}
