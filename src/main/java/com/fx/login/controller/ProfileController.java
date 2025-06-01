package com.fx.login.controller;

import com.fx.login.config.PendingSessionContext;
import com.fx.login.config.Router;
import com.fx.login.config.SessionContext;
import com.fx.login.model.PendingUser;
import com.fx.login.model.User;
import com.fx.login.service.PendingUserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;


@Component
@FxmlView("/ui/profile.fxml")
public class ProfileController implements Initializable {
    @FXML
    private Label lblName;

    @FXML
    private Label lblEmail;

    @FXML
    private Label lblSex;

    @FXML
    private Label lblNationality;

    @Autowired
    private PendingUserService pendingUserService;
    @Autowired
    private FxWeaver fxWeaver;


    @Autowired
    Router router;
    User currentUser;
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentUser = SessionContext.getInstance().getCurrentUser();
        lblName.setText(currentUser.getFullname());
        lblEmail.setText(currentUser.getEmail());
        lblSex.setText(currentUser.getSex());
        lblNationality.setText(currentUser.getCountry());
    }

    public void changeInfo(ActionEvent actionEvent) {
        PendingUser pendingUser = new PendingUser();
        pendingUser.setFullname(currentUser.getFullname());
        pendingUser.setCountry(currentUser.getCountry());
        pendingUser.setEmail(currentUser.getEmail());
        pendingUser.setPassword(currentUser.getPassword());
        pendingUser.setSex(currentUser.getSex());
        pendingUserService.save(pendingUser);

        Optional<PendingUser> userOpt = pendingUserService.findByEmail(pendingUser.getEmail());
        userOpt.ifPresent(user -> {
            // lưu người dùng vào Session
            PendingSessionContext.getInstance().setCurrentPendingUser(pendingUser);  // Tạo 1 session để lưu thông tin người đăng nhập

            try {
                Parent root = fxWeaver.loadView(ChangeInfoController.class);

                Stage stage = new Stage();
                stage.setOnCloseRequest(event -> {
                    String pendingEmail = pendingUser.getEmail();
                    boolean deleted = pendingUserService.deleteUserByEmail(pendingEmail);
                });
                stage.setScene(new Scene(root));
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void changePass(ActionEvent actionEvent) {
        PendingUser pendingUser = new PendingUser();
        pendingUser.setFullname(currentUser.getFullname());
        pendingUser.setCountry(currentUser.getCountry());
        pendingUser.setEmail(currentUser.getEmail());
        pendingUser.setPassword(currentUser.getPassword());
        pendingUser.setSex(currentUser.getSex());
        pendingUserService.save(pendingUser);

        Optional<PendingUser> userOpt = pendingUserService.findByEmail(pendingUser.getEmail());
        userOpt.ifPresent(user -> {
            // lưu người dùng vào Session
            PendingSessionContext.getInstance().setCurrentPendingUser(pendingUser);  // Tạo 1 session để lưu thông tin người đăng nhập

            try {
                Parent root = fxWeaver.loadView(ChangePassController.class);
                Stage stage = new Stage();

                stage.setOnCloseRequest(event -> {
                    String pendingEmail = pendingUser.getEmail();
                    boolean deleted = pendingUserService.deleteUserByEmail(pendingEmail);
                });
                stage.setScene(new Scene(root));
                stage.show();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
