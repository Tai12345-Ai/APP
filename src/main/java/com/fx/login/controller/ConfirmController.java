package com.fx.login.controller;

import com.fx.login.config.PendingSessionContext;
import com.fx.login.config.Router;
import com.fx.login.model.PendingUser;
import com.fx.login.model.User;
import com.fx.login.repo.PendingUserRepo;
import com.fx.login.service.PasswordResetService;
import com.fx.login.service.PendingUserService;
import com.fx.login.service.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@FxmlView("/ui/confirm.fxml")
public class ConfirmController {
    @Autowired
    private UserService userService;
    @Autowired
    private Router router;
    @FXML
    private Label lblConfirm;

    @FXML
    private PasswordField codeField;

    @Autowired
    private PasswordResetService resetService;

    @Autowired private PendingUserRepo pendingUserRepo;
    @Autowired private PendingUserService pendingUserService;
    PendingUser currentPendingUser;
    public void initialize() {
        currentPendingUser = PendingSessionContext.getInstance().getCurrentPendingUser();
    }
    @FXML
    public void handleVerifyCode() {
        String email = currentPendingUser.getEmail();
        String code = codeField.getText();
        if (resetService.verifyCode(email, code)) {
                try {
                    lblConfirm.setText("Mã chính xác. Tài khoản của bạn đã được xác nhận!");
                    lblConfirm.setTextFill(Color.web("#229abb"));
                    User user = new User();
                    user.setSex(currentPendingUser.getSex());
                    user.setCountry(currentPendingUser.getCountry());
                    user.setEmail(currentPendingUser.getEmail());
                    user.setFullname(currentPendingUser.getFullname());
                    user.setPassword(currentPendingUser.getPassword());
                    user.setDatecreated(LocalDateTime.now());
                    user.setRole(User.Role.Resident);
                    userService.save(user);
                } catch (Exception e) {
                    e.printStackTrace();
                }
        } else {
            lblConfirm.setText("Mã không chính xác!");
        }
    }

    public void loadLogin(MouseEvent mouseEvent) {
        String email = currentPendingUser.getEmail();
        boolean deleted = pendingUserService.deleteUserByEmail(email);
        router.navigateMouseEvent(LoginController.class,mouseEvent);
    }
}
