package com.fx.login.controller;


import com.fx.login.config.Router;
import com.fx.login.config.SessionContext;
import com.fx.login.model.User;
import com.fx.login.service.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;


@Component
@FxmlView("/ui/login.fxml")
public class LoginController implements Initializable {

    @FXML
    private Button btnLogin;

    @FXML
    private PasswordField password;

    @FXML
    private TextField username;

    @FXML
    private Label lblLogin;

    @Autowired
    private UserService userService;


    @Autowired
    private Router router;

    @FXML
    void onSignup(ActionEvent event) {

        router.navigate(SignupController.class, event);
    }

    @FXML
    private void login(ActionEvent event) throws IOException {
        if (userService.authenticate(getUsername(), getPassword())) {
            Optional<User> userOpt = userService.findByEmail(username.getText());
            userOpt.ifPresent(user -> {
                // lưu người dùng vào Session
                SessionContext.getInstance().setCurrentUser(user);  // Tạo 1 session để lưu thông tin người đăng nhập

                try {
                    router.navigate(DashboardController.class, event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });


        } else {
            if (username.getText().trim().isEmpty()||password.getText().trim().isEmpty()) lblLogin.setText("Vui lòng nhập đầy đủ thông tin!");
            else {
                lblLogin.setText("Địa chỉ email hoặc mật khẩu tài khoản không đúng!");
            }
        }
    }

    public String getPassword() {
        return password.getText();
    }

    public String getUsername() {
        return username.getText();
    }

    private Stage stage;

    public void initialize(URL location, ResourceBundle resources) {
        this.stage = new Stage();
    }


    public void handleForgotPassword(ActionEvent event) {
        router.navigate(ForgotPasswordController.class, event);
    }
}
