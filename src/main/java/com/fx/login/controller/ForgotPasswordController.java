package com.fx.login.controller;

import com.fx.login.config.Router;
import com.fx.login.config.SessionContext;
import com.fx.login.model.User;
import com.fx.login.service.EmailService;
import com.fx.login.service.PasswordResetService;
import com.fx.login.service.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@FxmlView("/ui/forgotpassword.fxml")
public class ForgotPasswordController {
    @FXML
    private TextField emailField;
    @FXML private TextField codeField;
    @FXML private Button sendCodeButton;
    @FXML private Button verifyButton;
    @FXML
    private Label lblNotification;

    @Autowired
    private Router router;
    @Autowired private EmailService emailService;
    @Autowired private PasswordResetService resetService;
    @Autowired
    private UserService userService;

    @FXML
    private void handleSendCode() {
        String email = emailField.getText();
        if (emailField.getText().trim().isEmpty()) lblNotification.setText("Vui lòng điền email!");
        else if (userService.existsByEmail(email)) {
            String code = resetService.generateCode();
            resetService.storeCode(email, code);
            emailService.sendVerificationCode(email, code);
            lblNotification.setText("Mã xác thực đã gửi!");
            lblNotification.setTextFill(Color.web("#229abb"));
        } else {
            lblNotification.setText("Email của người dùng không tồn tại!");
        }
    }

    @FXML
    private void handleVerifyCode(ActionEvent event) {
        String email = emailField.getText();
        String code = codeField.getText();
        if (resetService.verifyCode(email, code)) {
            Optional<User> userOpt = userService.findByEmail(emailField.getText());
            userOpt.ifPresent(user -> {
                // lưu người dùng vào Session
                SessionContext.getInstance().setCurrentUser(user);  // Tạo 1 session để lưu thông tin người đăng nhập

                try {
                    router.navigate(NewPasswordController.class, event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            lblNotification.setText("Mã không chính xác.");
        }
    }

    public void handleGoBack(MouseEvent mouseEvent) {
        router.navigateMouseEvent(LoginController.class,mouseEvent);
    }
}
