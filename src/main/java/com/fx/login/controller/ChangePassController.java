package com.fx.login.controller;

import com.fx.login.config.PendingSessionContext;
import com.fx.login.config.Router;
import com.fx.login.model.PendingUser;
import com.fx.login.service.EmailService;
import com.fx.login.service.PasswordResetService;
import com.fx.login.service.PendingUserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@FxmlView("/ui/changepass.fxml")
public class ChangePassController {
    @FXML
    private PasswordField newPassField;

    @FXML
    private PasswordField newPassRetypeField;

    @FXML
    private Text txtNotification;

    @Autowired
    PendingUserService pendingUserService;

    @Autowired
    Router router;

    @Autowired
    PasswordResetService resetService;

    @Autowired
    FxWeaver fxWeaver;

    @Autowired
    EmailService emailService;
    PendingUser currentPendingUser;
    public void initialize() {
        currentPendingUser = PendingSessionContext.getInstance().getCurrentPendingUser();
    }

    public void savePass(ActionEvent actionEvent) {
        if (!(newPassField.getText().equals(newPassRetypeField.getText()))) txtNotification.setText("Mật khẩu nhập lại phải trùng khớp!");
        else if (newPassField.getText().equals(currentPendingUser.getPassword())) txtNotification.setText("Mật khẩu mới phải khác mật khẩu cũ!");
        else {
            currentPendingUser.setPassword(newPassField.getText());
            pendingUserService.save(currentPendingUser);
            String email = currentPendingUser.getEmail();
            if (pendingUserService.existsByEmail(email)) {
                String code = resetService.generateCode();
                resetService.storeCode(email, code);
                emailService.sendPasswordVerificationCode(email, code);
            }

            Parent root = fxWeaver.loadView(ConfirmChangePassController.class);
            Node source = (Node) actionEvent.getSource();
            Stage stage = (Stage) source.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setOnCloseRequest(event -> {
                String pendingEmail = currentPendingUser.getEmail();
                boolean deleted = pendingUserService.deleteUserByEmail(pendingEmail);
            });
            stage.setResizable(false);
            stage.show();
        }
    }
}
