package com.fx.login.controller;

import com.fx.login.config.Router;
import com.fx.login.config.SessionContext;
import com.fx.login.model.NotificationType;
import com.fx.login.model.User;
import com.fx.login.service.NotificationService;
import com.fx.login.service.UserService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Component
@FxmlView("/ui/AdminSendNotificationView.fxml")
public class AdminSendNotificationController implements Initializable {

    // Components for recipient selection
    @FXML private ToggleGroup recipientToggleGroup;
    @FXML private RadioButton allRecipientsRadio;
    @FXML private RadioButton specificRecipientsRadio;
    @FXML private Label recipientIdsLabel;
    @FXML private TextField recipientIdsField;
    @FXML private Button selectRecipientsButton;
    @FXML private Label selectedRecipientsInfo;

    // Other form components
    @FXML private ComboBox<NotificationType> typeComboBox;
    @FXML private TextField subjectField;
    @FXML private TextArea contentArea;
    @FXML private TextField linkField;
    @FXML private CheckBox sendEmailCheckBox;
    @FXML private CheckBox sendInAppCheckBox;
    @FXML private Label statusLabel;
    @FXML private Button sendButton;

    // Tạo một static instance của service để sử dụng trong trường hợp khẩn cấp
    private static UserService staticUserService;

    @Autowired
    private Router router;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private UserService userService;

    private User currentAdmin;
    private List<User> selectedRecipients = new ArrayList<>();

    // Thêm phương thức này để cho phép DashboardController inject UserService
    public void injectUserService(UserService service) {
        if (service != null) {
            System.out.println("UserService đã được tiêm từ bên ngoài");
            this.userService = service;
            staticUserService = service;
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            // Kiểm tra và xử lý trường hợp userService là null
            if (userService == null) {
                System.err.println("UserService là null trong initialize() - cố gắng khắc phục");

                // Cách 1: Thử lấy từ ApplicationContext
                if (applicationContext != null) {
                    try {
                        userService = applicationContext.getBean(UserService.class);
                        System.out.println("Đã lấy UserService từ ApplicationContext");
                    } catch (Exception e) {
                        System.err.println("Không thể lấy UserService từ ApplicationContext: " + e.getMessage());
                    }
                }

                // Cách 2: Thử sử dụng static instance
                if (userService == null && staticUserService != null) {
                    System.out.println("Đang sử dụng staticUserService");
                    userService = staticUserService;
                }

                // Nếu vẫn không thể khởi tạo
                if (userService == null) {
                    System.err.println("Không thể khởi tạo UserService bằng bất kỳ cách nào");
                    showError("Lỗi: UserService không được khởi tạo. Một số chức năng sẽ không hoạt động đúng.");
                }
            } else {
                // Lưu vào static instance để dùng trong trường hợp khẩn cấp
                staticUserService = userService;
            }

            currentAdmin = SessionContext.getInstance().getCurrentUser();
            if (currentAdmin == null || currentAdmin.getRole() != User.Role.Admin) {
                showError("Lỗi: Bạn không có quyền truy cập chức năng này.");
                disableForm();
                return;
            }

            setupTypeComboBox();
            setupRecipientToggleGroup();
            clearStatusLabel();
        } catch (Exception e) {
            showError("Lỗi khởi tạo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupTypeComboBox() {
        if (typeComboBox != null) {
            typeComboBox.setItems(FXCollections.observableArrayList(NotificationType.values()));
            typeComboBox.setConverter(new javafx.util.StringConverter<NotificationType>() {
                @Override
                public String toString(NotificationType object) {
                    return object == null ? "" : object.getDisplayName();
                }

                @Override
                public NotificationType fromString(String string) {
                    return NotificationType.fromString(string);
                }
            });
        }
    }

    private void setupRecipientToggleGroup() {
        if (allRecipientsRadio != null) {
            allRecipientsRadio.setSelected(true);
        }

        // Add listeners to validate inputs properly
        if (recipientIdsField != null) {
            recipientIdsField.textProperty().addListener((observable, oldValue, newValue) -> {
                updateSelectedRecipientsInfo();
            });
        }
    }

    @FXML
    private void handleRecipientTypeChange(ActionEvent event) {
        try {
            boolean isSpecificRecipients = specificRecipientsRadio != null && specificRecipientsRadio.isSelected();

            // Enable/disable recipient selection controls
            if (recipientIdsField != null) recipientIdsField.setDisable(!isSpecificRecipients);
            if (selectRecipientsButton != null) selectRecipientsButton.setDisable(!isSpecificRecipients);
            if (recipientIdsLabel != null) recipientIdsLabel.setDisable(!isSpecificRecipients);

            // Clear specific recipient fields if switching to "all recipients"
            if (!isSpecificRecipients) {
                if (recipientIdsField != null) recipientIdsField.clear();
                selectedRecipients.clear();
                updateSelectedRecipientsInfo();
            }
        } catch (Exception e) {
            showError("Lỗi chuyển đổi loại người nhận: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSelectRecipients(ActionEvent event) {
        try {
            // Show recipient selection dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/RecipientSelectionDialog.fxml"));
            Parent root = loader.load();

            RecipientSelectionDialogController controller = loader.getController();
            if (controller != null) {
                controller.setPreSelectedIds(getSelectedRecipientIds());

                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle("Chọn người nhận thông báo");
                stage.setScene(new Scene(root));
                stage.showAndWait();

                // Get selected recipients after dialog is closed
                List<User> selected = controller.getSelectedRecipients();
                if (selected != null && !selected.isEmpty()) {
                    selectedRecipients = selected;

                    // Update recipient IDs field
                    String idString = selectedRecipients.stream()
                            .map(user -> String.valueOf(user.getId()))
                            .collect(Collectors.joining(","));
                    recipientIdsField.setText(idString);

                    updateSelectedRecipientsInfo();
                }
            }
        } catch (IOException e) {
            showError("Không thể mở dialog chọn người nhận: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<Long> getSelectedRecipientIds() {
        if (recipientIdsField == null || recipientIdsField.getText().trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return Arrays.stream(recipientIdsField.getText().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            return new ArrayList<>();
        }
    }

    private void updateSelectedRecipientsInfo() {
        if (selectedRecipientsInfo == null) return;

        try {
            // Kiểm tra và khôi phục userService nếu cần
            if (userService == null) {
                // Thử sử dụng static instance
                if (staticUserService != null) {
                    userService = staticUserService;
                } else {
                    selectedRecipientsInfo.setText("Không thể xác định số lượng người nhận (lỗi kết nối)");
                    selectedRecipientsInfo.setTextFill(Color.RED);
                    return;
                }
            }

            if (allRecipientsRadio != null && allRecipientsRadio.isSelected()) {
                try {
                    long residentCount = userService.getResidentCount();
                    selectedRecipientsInfo.setText(String.format("Thông báo sẽ được gửi đến tất cả %d cư dân", residentCount));
                    selectedRecipientsInfo.setTextFill(Color.BLUE);
                } catch (Exception e) {
                    selectedRecipientsInfo.setText("Không thể xác định số lượng cư dân: " + e.getMessage());
                    selectedRecipientsInfo.setTextFill(Color.RED);
                    e.printStackTrace();
                }
            } else {
                List<Long> ids = getSelectedRecipientIds();

                if (ids.isEmpty()) {
                    selectedRecipientsInfo.setText("Chưa có người nhận nào được chọn");
                    selectedRecipientsInfo.setTextFill(Color.RED);
                } else {
                    selectedRecipientsInfo.setText(String.format("Đã chọn %d người nhận", ids.size()));
                    selectedRecipientsInfo.setTextFill(Color.GREEN);
                }
            }
        } catch (Exception e) {
            selectedRecipientsInfo.setText("Lỗi khi xác định người nhận: " + e.getMessage());
            selectedRecipientsInfo.setTextFill(Color.RED);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSendNotification(ActionEvent event) {
        try {
            clearStatusLabel();

            // Kiểm tra và khôi phục userService nếu cần
            if (userService == null) {
                // Thử sử dụng static instance
                if (staticUserService != null) {
                    userService = staticUserService;
                } else {
                    showError("Lỗi: Dịch vụ người dùng chưa được khởi tạo. Vui lòng khởi động lại ứng dụng.");
                    return;
                }
            }

            if (currentAdmin == null) {
                showError("Lỗi: Không tìm thấy thông tin Admin.");
                return;
            }

            // Get form values
            NotificationType type = typeComboBox.getValue();
            String subject = subjectField.getText().trim();
            String content = contentArea.getText().trim();
            String link = linkField.getText().trim();
            boolean sendViaEmail = sendEmailCheckBox.isSelected();
            boolean sendViaInApp = sendInAppCheckBox.isSelected();

            // Validate form
            if (!validateForm(type, subject, content, sendViaEmail, sendViaInApp)) {
                return;
            }

            // Get recipients
            List<User> recipientUsers = getRecipientUsers();
            if (recipientUsers.isEmpty()) {
                showError("Không xác định được người nhận hợp lệ.");
                return;
            }

            // Description for notification history
            String recipientTargetDescription =
                    (allRecipientsRadio != null && allRecipientsRadio.isSelected())
                            ? "ALL"
                            : recipientIdsField.getText().trim();

            try {
                notificationService.createAndSendNotification(
                        currentAdmin,
                        recipientUsers,
                        recipientTargetDescription,
                        type,
                        subject,
                        content,
                        link.isEmpty() ? null : link,
                        sendViaEmail,
                        sendViaInApp
                );
                showSuccess("Thông báo đã được gửi thành công tới " + recipientUsers.size() + " người nhận!");
            } catch (Exception e) {
                System.err.println("Lỗi khi gửi thông báo: " + e.getMessage());
                e.printStackTrace();
                showError("Gửi thông báo thất bại. Chi tiết: " + e.getMessage());
            }
        } catch (Exception e) {
            showError("Lỗi khi xử lý yêu cầu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean validateForm(NotificationType type, String subject, String content,
                                 boolean sendViaEmail, boolean sendViaInApp) {
        if (type == null) {
            showError("Vui lòng chọn loại thông báo.");
            typeComboBox.requestFocus();
            return false;
        }
        if (subject.isEmpty()) {
            showError("Vui lòng nhập tiêu đề thông báo.");
            subjectField.requestFocus();
            return false;
        }
        if (content.isEmpty()) {
            showError("Vui lòng nhập nội dung thông báo.");
            contentArea.requestFocus();
            return false;
        }
        if (!sendViaEmail && !sendViaInApp) {
            showError("Vui lòng chọn ít nhất một kênh để gửi thông báo (Email hoặc Hệ thống).");
            return false;
        }

        // Validate recipients
        if (specificRecipientsRadio != null && specificRecipientsRadio.isSelected()) {
            String recipientInput = recipientIdsField.getText().trim();
            if (recipientInput.isEmpty()) {
                showError("Vui lòng nhập ID người nhận hoặc sử dụng nút 'Chọn cư dân'.");
                recipientIdsField.requestFocus();
                return false;
            }

            try {
                List<Long> ids = Arrays.stream(recipientInput.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::parseLong)
                        .collect(Collectors.toList());

                if (ids.isEmpty()) {
                    showError("Vui lòng nhập ít nhất một ID người nhận hợp lệ.");
                    recipientIdsField.requestFocus();
                    return false;
                }
            } catch (NumberFormatException e) {
                showError("ID người nhận phải là số (ví dụ: 1,2,3).");
                recipientIdsField.requestFocus();
                return false;
            }
        }

        return true;
    }

    private List<User> getRecipientUsers() {
        List<User> recipientUsers = new ArrayList<>();

        // Kiểm tra và khôi phục userService nếu cần
        if (userService == null) {
            // Thử sử dụng static instance
            if (staticUserService != null) {
                userService = staticUserService;
            } else {
                showError("Lỗi: Dịch vụ người dùng chưa được khởi tạo. Vui lòng khởi động lại ứng dụng.");
                return recipientUsers;
            }
        }

        try {
            if (allRecipientsRadio != null && allRecipientsRadio.isSelected()) {
                try {
                    recipientUsers = userService.findAllResidents();
                } catch (Exception e) {
                    System.err.println("Lỗi khi lấy danh sách cư dân: " + e.getMessage());
                    e.printStackTrace();
                    showError("Không thể lấy danh sách cư dân: " + e.getMessage());
                }
            } else if (specificRecipientsRadio != null && specificRecipientsRadio.isSelected()) {
                String recipientInput = recipientIdsField.getText().trim();

                try {
                    List<Long> recipientIds = Arrays.stream(recipientInput.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(Long::parseLong)
                            .collect(Collectors.toList());

                    if (!recipientIds.isEmpty()) {
                        try {
                            recipientUsers = userService.findResidentsByIds(recipientIds);

                            // Check if all requested IDs were found
                            if (recipientUsers.size() != recipientIds.size()) {
                                List<Long> foundIds = recipientUsers.stream()
                                        .map(User::getId)
                                        .collect(Collectors.toList());

                                List<Long> notFoundIds = recipientIds.stream()
                                        .filter(id -> !foundIds.contains(id))
                                        .collect(Collectors.toList());

                                showError("Không tìm thấy hoặc không phải Cư dân cho các ID: " + notFoundIds);
                            }
                        } catch (Exception e) {
                            System.err.println("Lỗi khi tìm cư dân theo ID: " + e.getMessage());
                            e.printStackTrace();
                            showError("Không thể tìm kiếm cư dân theo ID: " + e.getMessage());
                        }
                    }
                } catch (NumberFormatException e) {
                    showError("ID người nhận phải là số (ví dụ: 1,2,3).");
                }
            }
        } catch (Exception e) {
            showError("Lỗi khi lấy danh sách người nhận: " + e.getMessage());
            e.printStackTrace();
        }

        return recipientUsers;
    }

    @FXML
    private void clearForm(ActionEvent event) {
        try {
            if (allRecipientsRadio != null) allRecipientsRadio.setSelected(true);
            if (specificRecipientsRadio != null) specificRecipientsRadio.setSelected(false);
            if (recipientIdsField != null) {
                recipientIdsField.clear();
                recipientIdsField.setDisable(true);
            }
            if (selectRecipientsButton != null) selectRecipientsButton.setDisable(true);
            if (typeComboBox != null) typeComboBox.getSelectionModel().clearSelection();
            if (subjectField != null) subjectField.clear();
            if (contentArea != null) contentArea.clear();
            if (linkField != null) linkField.clear();
            if (sendEmailCheckBox != null) sendEmailCheckBox.setSelected(true);
            if (sendInAppCheckBox != null) sendInAppCheckBox.setSelected(true);

            selectedRecipients.clear();
            updateSelectedRecipientsInfo();
            clearStatusLabel();
        } catch (Exception e) {
            showError("Lỗi khi xóa form: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleShowHistory(ActionEvent event) {
        try {
            // Lấy Stage hiện tại
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // Load dashboard view qua Spring
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/dashboard.fxml"));
            loader.setControllerFactory(applicationContext::getBean); // <-- inject Spring
            Parent root = loader.load();

            // Thiết lập Scene và Stage
            Scene scene = new Scene(root);
            currentStage.setScene(scene);

            // Lấy controller dashboard để thực hiện các thao tác tiếp theo
            DashboardController dashboardController = loader.getController();

            // Load AdminNotificationHistoryView vào ScrollPane qua Spring
            FXMLLoader historyLoader = new FXMLLoader(getClass().getResource("/ui/AdminNotificationHistoryView.fxml"));
            historyLoader.setControllerFactory(applicationContext::getBean); // <-- inject Spring
            Parent historyView = historyLoader.load();
            dashboardController.getScrollPane().setContent(historyView);

            currentStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Không thể mở lịch sử: " + e.getMessage());
        }
    }

    private void disableForm() {
        if (allRecipientsRadio != null) allRecipientsRadio.setDisable(true);
        if (specificRecipientsRadio != null) specificRecipientsRadio.setDisable(true);
        if (recipientIdsField != null) recipientIdsField.setDisable(true);
        if (selectRecipientsButton != null) selectRecipientsButton.setDisable(true);
        if (typeComboBox != null) typeComboBox.setDisable(true);
        if (subjectField != null) subjectField.setDisable(true);
        if (contentArea != null) contentArea.setDisable(true);
        if (linkField != null) linkField.setDisable(true);
        if (sendEmailCheckBox != null) sendEmailCheckBox.setDisable(true);
        if (sendInAppCheckBox != null) sendInAppCheckBox.setDisable(true);
        if (sendButton != null) sendButton.setDisable(true);
    }

    private void showSuccess(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setTextFill(Color.GREEN);
            statusLabel.setVisible(true);
            statusLabel.setManaged(true);
        }
    }

    private void showError(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setTextFill(Color.RED);
            statusLabel.setVisible(true);
            statusLabel.setManaged(true);
        }
    }

    private void clearStatusLabel() {
        if (statusLabel != null) {
            statusLabel.setText("");
            statusLabel.setVisible(false);
            statusLabel.setManaged(false);
        }
    }

    // Thêm vào AdminSendNotificationController.java
    public void prepopulateFromExisting(com.fx.login.model.Notification notification) {
        if (notification == null) return;

        // Nếu sử dụng RadioButton cho việc chọn người nhận
        if (allRecipientsRadio != null && specificRecipientsRadio != null) {
            if ("ALL".equalsIgnoreCase(notification.getRecipientTargetDescription())) {
                allRecipientsRadio.setSelected(true);
                specificRecipientsRadio.setSelected(false);
                if (recipientIdsField != null) {
                    recipientIdsField.setDisable(true);
                    recipientIdsField.clear();
                }
                if (selectRecipientsButton != null) {
                    selectRecipientsButton.setDisable(true);
                }
            } else {
                allRecipientsRadio.setSelected(false);
                specificRecipientsRadio.setSelected(true);
                if (recipientIdsField != null) {
                    recipientIdsField.setDisable(false);
                    recipientIdsField.setText(notification.getRecipientTargetDescription());
                }
                if (selectRecipientsButton != null) {
                    selectRecipientsButton.setDisable(false);
                }
            }
        } else if (recipientIdsField != null) {
            // Nếu chỉ có TextField cho người nhận
            recipientIdsField.setText(notification.getRecipientTargetDescription());
        }

        // Điền các thông tin khác
        if (typeComboBox != null) {
            typeComboBox.setValue(notification.getNotificationType());
        }

        if (subjectField != null) {
            subjectField.setText(notification.getEmailSubject());
        }

        if (contentArea != null) {
            contentArea.setText(notification.getMessageContent());
        }

        if (linkField != null) {
            linkField.setText(notification.getLinkUrl());
        }

        // Xử lý checkboxes - có thể cần điều chỉnh logic này dựa trên cách bạn lưu trữ
        // thông tin về kênh gửi thông báo trong database
        if (sendEmailCheckBox != null) {
            sendEmailCheckBox.setSelected(true); // Mặc định bật cả hai
        }

        if (sendInAppCheckBox != null) {
            sendInAppCheckBox.setSelected(true); // Mặc định bật cả hai
        }

        // Thêm chú thích đây là thông báo được tạo lại
        if (statusLabel != null) {
            statusLabel.setText("Đã điền thông tin từ thông báo ID: " + notification.getId());
            statusLabel.setTextFill(Color.BLUE);
            statusLabel.setVisible(true);
        }
    }

    // Thêm getter cho staticUserService
    public static UserService getStaticUserService() {
        return staticUserService;
    }
}