package com.fx.login.controller;

import com.fx.login.config.Router;
import com.fx.login.config.SessionContext;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.TilePane;
import javafx.scene.text.Text;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import com.fx.login.model.User;

@Component
@FxmlView("/ui/dashboard.fxml")
public class DashboardController implements Initializable {
    @Autowired
    private Router entrance;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private TilePane btnResident;
    @FXML
    private AnchorPane contentPane;

    @Autowired
    FxWeaver fxWeaver;
    @FXML private Text welcomeText;

    User currentUser;

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = SessionContext.getInstance().getCurrentUser();
        if (currentUser != null) {
            welcomeText.setText(currentUser.getFullname());
        }

        try {
            AnchorPane scene2 = FXMLLoader.load(getClass().getResource("/ui/homepage.fxml"));
            scrollPane.setContent(scene2);
        } catch (IOException e) {
            System.err.println("Không thể tải homepage: " + e.getMessage());
        }
    }

    @FXML
    public void loadResident(MouseEvent mouseEvent) throws IOException {
        AnchorPane scene2 = FXMLLoader.load(getClass().getResource("/ui/resident.fxml"));
        scrollPane.setContent(scene2);
    }

    @FXML
    private void logOut(ActionEvent event) throws IOException {
        entrance.navigate(LoginController.class, event);
    }

    @FXML
    public void loadTrangChu(MouseEvent mouseEvent) throws IOException {
        AnchorPane scene2 = FXMLLoader.load(getClass().getResource("/ui/homepage.fxml"));
        scrollPane.setContent(scene2);
    }

    @FXML
    public void loadFee(MouseEvent mouseEvent) throws IOException {
        User currentUser = SessionContext.getInstance().getCurrentUser();

        try {
            if (currentUser != null && currentUser.getRole() == User.Role.Admin) {
                // Admin: Load giao diện quản lý khoản thu
                Parent scene2 = fxWeaver.loadView(AdminFeeController.class);
                scrollPane.setContent(scene2);
                System.out.println("Loaded AdminFeeController for admin user");
            } else {
                // Resident: Load giao diện xem khoản phí
                Parent scene2 = fxWeaver.loadView(ResidentFeeController.class);
                scrollPane.setContent(scene2);
                System.out.println("Loaded ResidentFeeController for resident user");
            }
        } catch (Exception e) {
            System.err.println("Error loading fee view: " + e.getMessage());
            e.printStackTrace();
            
            // Hiển thị thông báo lỗi cho người dùng
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText("Không thể tải giao diện khoản phí");
            alert.setContentText("Chi tiết lỗi: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    public void loadPayment(MouseEvent mouseEvent) throws IOException {
        User currentUser = SessionContext.getInstance().getCurrentUser();

        try {
            if (currentUser != null && currentUser.getRole() == User.Role.Admin) {
                // Admin: Load giao diện quản lý thanh toán đầy đủ
                Parent scene2 = fxWeaver.loadView(AdminPaymentController.class);
                scrollPane.setContent(scene2);
            } else {
                // Resident: Load giao diện thanh toán đơn giản
                Parent scene2 = fxWeaver.loadView(ResidentPaymentController.class);
                scrollPane.setContent(scene2);
            }
        } catch (Exception e) {
            System.err.println("Error loading payment view: " + e.getMessage());
            e.printStackTrace();
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText("Không thể tải giao diện thanh toán");
            alert.setContentText("Chi tiết lỗi: " + e.getMessage());
            alert.showAndWait();
        }
    }

    public void loadProfile(MouseEvent mouseEvent) throws IOException {
        try {
            AnchorPane scene2 = fxWeaver.loadView(ProfileController.class);
            scrollPane.setContent(scene2);
        } catch (Exception e) {
            System.err.println("Error loading profile: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadFeedBack(MouseEvent mouseEvent) throws IOException {
        try {
            AnchorPane scene2 = FXMLLoader.load(getClass().getResource("/ui/feedback.fxml"));
            scrollPane.setContent(scene2);
        } catch (Exception e) {
            System.err.println("Error loading feedback: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public ScrollPane getScrollPane() {
        return scrollPane;
    }

    @FXML
    public void loadNotificationView(MouseEvent mouseEvent) throws IOException {
        try {
            String fxmlPath = currentUser.getRole() == User.Role.Admin
                    ? "/ui/AdminSendNotificationView.fxml"
                    : "/ui/ResidentNotificationsView.fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(applicationContext::getBean);
            Parent notificationView = loader.load();

            scrollPane.setContent(notificationView);
        } catch (Exception e) {
            System.err.println("Error loading notification view: " + e.getMessage());
            e.printStackTrace();
        }
    }
}