package com.fx.login.controller;

import com.fx.login.config.PendingSessionContext;
import com.fx.login.config.Router;
import com.fx.login.config.SessionContext;
import com.fx.login.dto.Countries;
import com.fx.login.dto.Sex;
import com.fx.login.model.PendingUser;
import com.fx.login.model.User;
import com.fx.login.service.EmailService;
import com.fx.login.service.PasswordResetService;
import com.fx.login.service.PendingUserService;
import com.fx.login.service.UserService;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@FxmlView("/ui/changeinfo.fxml")
public class ChangeInfoController {
    @FXML
    private Text txtNotification;

    @FXML
    private TextField newName;

    @FXML
    private ComboBox newSex;

    @FXML
    private TextField newEmail;

    @FXML
    private ComboBox newNationality;

    @FXML private Button btnSave;
    @Autowired private PendingUserService pendingUserService;
    @Autowired private PasswordResetService resetService;
    @Autowired private EmailService emailService;
    @Autowired private Router router;
    @Autowired
    UserService userService;
    PendingUser currentPendingUser;

    @Autowired
    FxWeaver fxWeaver;
    User currentUser;
    public void initialize() {
        currentPendingUser = PendingSessionContext.getInstance().getCurrentPendingUser();
        currentUser = SessionContext.getInstance().getCurrentUser();
        newSex.setItems(Sex.obsList());
        newNationality.setItems((Countries.obsList()));
    }

    public void saveInfo(ActionEvent actionEvent) {
        if (!(newName.getText().trim().isEmpty())) currentPendingUser.setFullname(newName.getText());
        if (!(newSex.getValue() == null)) currentPendingUser.setSex(newSex.getSelectionModel().getSelectedItem().toString());
        if (!(newNationality.getValue() == null)) currentPendingUser.setCountry(newNationality.getSelectionModel().getSelectedItem().toString());
        if (!(newEmail.getText().trim().isEmpty() || newEmail.getText().equals(currentPendingUser.getEmail()))) {
            currentPendingUser.setEmail(newEmail.getText());
            pendingUserService.save(currentPendingUser);
            String email = newEmail.getText();
            String code = resetService.generateCode();
            resetService.storeCode(email, code);
            emailService.sendInfoVerificationCode(email, code);


            Parent root = fxWeaver.loadView(ConfirmChangeInfoController.class);
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

        else {
            currentUser.setSex(currentPendingUser.getSex());
            currentUser.setCountry(currentPendingUser.getCountry());
            currentUser.setEmail(currentPendingUser.getEmail());
            currentUser.setFullname(currentPendingUser.getFullname());
            currentUser.setPassword(currentPendingUser.getPassword());
            currentUser.setRole(User.Role.Resident);
            userService.save(currentUser);
            String pendingEmail = currentPendingUser.getEmail();    //xóa PendingUser
            boolean deleted = pendingUserService.deleteUserByEmail(pendingEmail);
            txtNotification.setText("Lưu thông tin thành công!");
            txtNotification.setFill(Color.web("#229abb"));

            PauseTransition delay = new PauseTransition(Duration.seconds(5));
            delay.setOnFinished(event -> {
                // Lấy Stage từ button
                Stage stage = (Stage) btnSave.getScene().getWindow();
                stage.close();
            });

            delay.play();
        }
    }
}
