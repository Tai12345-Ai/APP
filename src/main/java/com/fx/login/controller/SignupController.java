package com.fx.login.controller;

import com.fx.login.config.PendingSessionContext;
import com.fx.login.config.Router;
import com.fx.login.dto.Countries;
import com.fx.login.dto.Sex;
import com.fx.login.model.PendingUser;
import com.fx.login.model.User;
import com.fx.login.service.EmailService;
import com.fx.login.service.PasswordResetService;
import com.fx.login.service.PendingUserService;
import com.fx.login.service.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;

@Component
@FxmlView("/ui/signup.fxml")
public class SignupController implements Initializable {
    @Autowired
    UserService userService;

    @Autowired
    PendingUserService pendingUserService;

    @Autowired
    Router router;


    // the name of the nodes are coming from fxml fx:id -> it is identity associated to component in fxml to build a controller
    @FXML
    private TextField txtFullName;
    @FXML
    private Label lblNameError;

    @FXML
    private ComboBox boxSex;
    @FXML
    private Label lblPassRetype;
    @FXML
    private TextField txtMail;
    @FXML
    private Label lblMailError;

    @FXML
    private TextField txtPass;

    @FXML
    private TextField txtPassRetype;

    @FXML
    private Label lblPassError;

    @FXML
    private ComboBox boxCountry;
    @FXML
    private Label lblCountryError;

    @FXML
    private TextField txtCity;
    @FXML
    private Label lblCityError;

    @FXML
    private Label lblError;     // General Error Label

    @Autowired private EmailService emailService;
    @Autowired private PasswordResetService resetService;
    @FXML
    private void handleRegisterButtonAction(ActionEvent event) {
        String fullName;
        String mail;
        String pass;
        String country;
        String sex;

        // Check if any of the text field is empty
        ArrayList<TextField> txtList = new ArrayList<>();
        txtList.add(txtFullName);
        txtList.add(txtMail);
        txtList.add(txtPass);
        // iterate the textField nodes
        for (TextField nodes : txtList) {
            if (nodes.getText().isEmpty()) {
                lblError.setText("Hãy điền đầy đủ thông tin!");
            }

        }
        if (boxCountry.getSelectionModel().isEmpty()) {     // check if a country is selected
            lblCountryError.setText("Hãy chọn 1 quốc tịch trong danh sách.");
        } else if (isValidEmailAddress(txtMail.getText()) == false) {     // check if the mail address is a valid address
            lblMailError.setText("Hãy điền 1 email hợp lệ!");
            lblError.setText("");
        } else if (userService.findByEmail(txtMail.getText()).isPresent()) {
            lblMailError.setText("Email đã tồn tại! Vui lòng chọn email khác.");
        } else if (!txtPass.getText().equals(txtPassRetype.getText())) {
            lblPassRetype.setText("Mật khẩu nhập lại không khớp!");
        } else {
            lblCountryError.setText("");
            lblMailError.setText("");
            lblPassRetype.setText("");
            // store the user's inputs
            fullName = txtFullName.getText();
            mail = txtMail.getText();
            pass = txtPass.getText();
            country = boxCountry.getSelectionModel().getSelectedItem().toString();
            sex = boxSex.getSelectionModel().getSelectedItem().toString();

            PendingUser user = new PendingUser();
            user.setSex(sex);
            user.setCountry(country);
            user.setEmail(mail);
            user.setFullname(fullName);
            user.setPassword(pass);
            user.setDatecreated(LocalDateTime.now());
            user.setRole(User.Role.Resident);
            pendingUserService.save(user);

            String email = txtMail.getText();
            if (pendingUserService.existsByEmail(email)) {
                String code = resetService.generateCode();
                resetService.storeCode(email, code);
                emailService.sendAccountVerificationCode(email, code);
            }

            Optional<PendingUser> userOpt = pendingUserService.findByEmail(txtMail.getText());
            userOpt.ifPresent(pendingUser -> {
                // lưu người dùng vào Session
                PendingSessionContext.getInstance().setCurrentPendingUser(pendingUser);  // Tạo 1 session để lưu thông tin người đăng nhập

                try {
                    router.navigate(ConfirmController.class, event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

    }


    public static boolean isValidEmailAddress(String email) {
        boolean result = true;
        try {
            if (email == null || !email.matches("^\\S+@\\S+\\.\\S+$")) {
                result = false;
            }
        } catch (Exception ex) {
            result = false;
        }
        return result;
    }
    @FXML
    void onLogin(ActionEvent event) {
        router.navigate(LoginController.class, event);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO

        // display the list of countries
        boxCountry.setItems(Countries.obsList());
        boxSex.setItems(Sex.obsList());

    }

}
