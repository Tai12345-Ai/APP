package com.fx.login.controller;

import com.fx.login.config.PendingSessionContext;
import com.fx.login.config.SessionContext;
import com.fx.login.model.PendingUser;
import com.fx.login.model.User;
import com.fx.login.service.PasswordResetService;
import com.fx.login.service.PendingUserService;
import com.fx.login.service.UserService;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@FxmlView("/ui/confirmchangeinfo.fxml")
public class ConfirmChangeInfoController {
    @FXML private Text lblNotification;
    @FXML private PasswordField codeField;
    @FXML private Button verifyButton;

    @Autowired private PasswordResetService resetService;
    @Autowired private UserService userService;
    @Autowired private PendingUserService pendingUserService;
    PendingUser currentPendingUser;
    User currentUser;
    public void initialize() {
        currentPendingUser = PendingSessionContext.getInstance().getCurrentPendingUser();
        currentUser = SessionContext.getInstance().getCurrentUser();
    }

    public void verifyCode() {
        String email = currentPendingUser.getEmail();
        String code = codeField.getText();
        if (resetService.verifyCode(email, code)) {
            try {
                lblNotification.setText("Mã chính xác. Tài khoản của bạn đã được xác nhận!");
                lblNotification.setFill(Color.web("#229abb"));
                currentUser.setSex(currentPendingUser.getSex());
                currentUser.setCountry(currentPendingUser.getCountry());
                currentUser.setEmail(currentPendingUser.getEmail());
                currentUser.setFullname(currentPendingUser.getFullname());
                currentUser.setPassword(currentPendingUser.getPassword());
                currentUser.setRole(User.Role.Resident);
                userService.save(currentUser);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            lblNotification.setText("Mã không chính xác.");
        }
        String pendingEmail = currentPendingUser.getEmail();
        boolean deleted = pendingUserService.deleteUserByEmail(pendingEmail);
        PauseTransition delay = new PauseTransition(Duration.seconds(5));
        delay.setOnFinished(event -> {
            // Lấy Stage từ button
            Stage stage = (Stage) verifyButton.getScene().getWindow();
            stage.close();
        });

        delay.play();
    }
}
