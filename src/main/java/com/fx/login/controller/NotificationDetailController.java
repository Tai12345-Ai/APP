package com.fx.login.controller;

import com.fx.login.model.Notification;
import com.fx.login.model.NotificationRecipient;
import com.fx.login.service.NotificationService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class NotificationDetailController implements Initializable {

    @FXML private Label idLabel;
    @FXML private Label sentAtLabel;
    @FXML private Label senderLabel;
    @FXML private Label recipientLabel;
    @FXML private Label typeLabel;
    @FXML private TextField subjectField;
    @FXML private TextArea contentArea;
    @FXML private TextField linkField;
    @FXML private Label channelsLabel;
    @FXML private Label statusLabel;
    @FXML private Button closeButton;

    private NotificationService notificationService;
    private Long notificationId;

    private static ApplicationContext applicationContext;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public NotificationDetailController() {
        // Default constructor for FXML
    }

    @Autowired
    public NotificationDetailController(NotificationService notificationService, ApplicationContext context) {
        this.notificationService = notificationService;
        NotificationDetailController.applicationContext = context;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Đảm bảo service được inject
        if (notificationService == null && applicationContext != null) {
            notificationService = applicationContext.getBean(NotificationService.class);
        }

        // Thiết lập các TextFields và TextArea chỉ đọc
        if (subjectField != null) subjectField.setEditable(false);
        if (contentArea != null) contentArea.setEditable(false);
        if (linkField != null) linkField.setEditable(false);

        // Khi đã có notificationId, load thông tin
        if (notificationId != null) {
            loadNotificationDetails();
        }
    }

    public void setNotificationId(Long id) {
        this.notificationId = id;
        // Nếu đã initialize xong, load thông tin
        if (idLabel != null) {
            loadNotificationDetails();
        }
    }

    private void loadNotificationDetails() {
        try {
            if (notificationService == null) {
                setStatus("Lỗi: Dịch vụ thông báo không khả dụng", true);
                return;
            }

            if (notificationId == null) {
                setStatus("Lỗi: Không tìm thấy ID thông báo", true);
                return;
            }

            Notification notification = notificationService.getNotificationById(notificationId);
            if (notification == null) {
                setStatus("Không tìm thấy thông báo với ID: " + notificationId, true);
                return;
            }

            // Hiển thị thông tin cơ bản
            idLabel.setText(notification.getId().toString());
            sentAtLabel.setText(notification.getSentAt().format(dateTimeFormatter));

            String senderInfo = notification.getSenderAdmin() != null
                    ? notification.getSenderAdmin().getId() + " - " + notification.getSenderAdmin().getFullname()
                    : "Không có thông tin";
            senderLabel.setText(senderInfo);

            // Xử lý thông tin người nhận
            String recipientInfo;
            if ("ALL".equalsIgnoreCase(notification.getRecipientTargetDescription())) {
                recipientInfo = "Tất cả cư dân";
            } else {
                // Lấy danh sách người nhận từ recipients hoặc từ mô tả
                List<NotificationRecipient> recipients = notification.getRecipients();
                if (recipients != null && !recipients.isEmpty()) {
                    recipientInfo = recipients.size() + " người nhận đã chọn";
                } else {
                    recipientInfo = notification.getRecipientTargetDescription();
                }
            }
            recipientLabel.setText(recipientInfo);

            // Loại thông báo
            typeLabel.setText(notification.getNotificationType().getDisplayName());

            // Nội dung chi tiết
            subjectField.setText(notification.getEmailSubject());
            contentArea.setText(notification.getMessageContent());
            linkField.setText(notification.getLinkUrl() != null ? notification.getLinkUrl() : "");

            // Kênh gửi
            boolean sentViaEmail = false;
            boolean sentViaApp = false;

            if (notification.getRecipients() != null && !notification.getRecipients().isEmpty()) {
                // Kiểm tra kênh gửi từ NotificationRecipient nếu có
                NotificationRecipient firstRecipient = notification.getRecipients().get(0);
                sentViaEmail = firstRecipient.isSentViaEmail();
                sentViaApp = firstRecipient.isSentViaApp();
            }

            StringBuilder channelsInfo = new StringBuilder();
            if (sentViaEmail) channelsInfo.append("Email");
            if (sentViaApp) {
                if (channelsInfo.length() > 0) channelsInfo.append(", ");
                channelsInfo.append("Hệ thống");
            }
            if (channelsInfo.length() == 0) channelsInfo.append("Không xác định");

            channelsLabel.setText(channelsInfo.toString());

            // Hiển thị thông tin chi tiết về người nhận nếu có
            if (notification.getRecipients() != null && !notification.getRecipients().isEmpty()) {
                // Có thể thêm một TextArea để hiển thị danh sách người nhận chi tiết nếu cần
            }

        } catch (Exception e) {
            e.printStackTrace();
            setStatus("Lỗi khi tải thông tin thông báo: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    private void setStatus(String message, boolean isError) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: blue;");
            statusLabel.setVisible(true);
        }
    }
}