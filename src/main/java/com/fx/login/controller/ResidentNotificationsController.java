package com.fx.login.controller;

import com.fx.login.config.Router;
import com.fx.login.config.SessionContext;
import com.fx.login.dto.ResidentNotificationViewDTO;
import com.fx.login.model.User;
import com.fx.login.service.NotificationService;
import javafx.application.HostServices;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

@Controller
@Component
@FxmlView("/ui/ResidentNotificationsView.fxml") // Đảm bảo đường dẫn FXML đúng
public class ResidentNotificationsController implements Initializable {

    @FXML private VBox rootPane;
    @FXML private CheckBox unreadOnlyCheckBox;
    @FXML private ListView<ResidentNotificationViewDTO> notificationsListView;
    @FXML private Label pageInfoLabel;
    @FXML private Button prevPageButton;
    @FXML private Button nextPageButton;
    @FXML private Label statusLabel;

    @Autowired
    private Router router;
    @Autowired
    private NotificationService notificationService;
    @Autowired(required = false) // HostServices có thể không được inject nếu không chạy trong môi trường JavaFX đầy đủ
    private HostServices hostServices;


    private User currentResident;
    private final ObservableList<ResidentNotificationViewDTO> notificationObservableList = FXCollections.observableArrayList();
    private static final int PAGE_SIZE = 10;
    private int currentPage = 0;
    private long totalNotifications = 0;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentResident = SessionContext.getInstance().getCurrentUser();
        if (currentResident == null || currentResident.getRole() != User.Role.Resident) {
            statusLabel.setText("Lỗi: Bạn không có quyền truy cập hoặc chưa đăng nhập.");
            statusLabel.setTextFill(Color.RED);
            // Disable controls
            unreadOnlyCheckBox.setDisable(true);
            notificationsListView.setDisable(true);
            // ... các nút khác
            return;
        }

        setupListView();
        loadNotifications();

        // Listener cho việc chọn item trong ListView để đánh dấu đã đọc và hiển thị chi tiết
        notificationsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                handleNotificationSelected(newSelection);
            }
        });
    }

    private void setupListView() {
        notificationsListView.setItems(notificationObservableList);
        notificationsListView.setCellFactory(listView -> new NotificationListCell());
    }

    @FXML
    private void handleReloadNotifications(ActionEvent event) {
        currentPage = 0;
        loadNotifications();
    }

    @FXML
    private void handleMarkAllAsRead(ActionEvent event) {
        if (currentResident == null) return;
        try {
            int markedCount = notificationService.markAllAsReadForResident(currentResident);
            showSuccess(markedCount + " thông báo đã được đánh dấu đã đọc.");
            loadNotifications(); // Tải lại để cập nhật UI
        } catch (Exception e) {
            e.printStackTrace();
            showError("Lỗi khi đánh dấu đã đọc: " + e.getMessage());
        }
    }

    @FXML
    private void handleFilterChanged(ActionEvent event) {
        currentPage = 0;
        loadNotifications();
    }
    private void loadNotifications() {
        if (currentResident == null) return;
        clearStatusLabel();

        boolean unreadOnly = unreadOnlyCheckBox.isSelected();
        // Sửa dòng này!
        Pageable pageable = PageRequest.of(currentPage, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "notification.sentAt")); // ĐÚNG

        try {
            Page<ResidentNotificationViewDTO> notificationPage = notificationService.getNotificationsForResident(
                    currentResident, unreadOnly, pageable
            );
            notificationObservableList.setAll(notificationPage.getContent());
            totalNotifications = notificationPage.getTotalElements();
            updatePaginationControls(notificationPage);

            if (notificationObservableList.isEmpty()) {
                notificationsListView.setPlaceholder(new Label(unreadOnly ? "Không có thông báo chưa đọc." : "Không có thông báo nào."));
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Lỗi khi tải thông báo: " + e.getMessage());
            notificationObservableList.clear();
            notificationsListView.setPlaceholder(new Label("Lỗi khi tải dữ liệu."));
        }
    }

    private void handleNotificationSelected(ResidentNotificationViewDTO selectedNotification) {
        // 1. Đánh dấu đã đọc (nếu chưa đọc)
        if (!selectedNotification.isRead()) {
            try {
                boolean success = notificationService.markAsRead(currentResident, selectedNotification.getNotificationRecipientId());
                if (success) {
                    selectedNotification.setRead(true); // Cập nhật DTO local
                    // Cần refresh item trong listview hoặc reload list
                    // Cách đơn giản là tìm và cập nhật item trong observableList
                    int index = notificationObservableList.indexOf(selectedNotification);
                    if (index != -1) {
                        notificationObservableList.set(index, selectedNotification); // Force refresh
                    } else {
                        loadNotifications(); // Hoặc tải lại cả trang nếu khó tìm index
                    }
                }
            } catch (Exception e) {
                System.err.println("Lỗi khi đánh dấu đã đọc: " + e.getMessage());
            }
        }

        // 2. Hiển thị chi tiết thông báo (ví dụ: trong Dialog)
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Chi Tiết Thông Báo");
        alert.setHeaderText(selectedNotification.getEmailSubject());

        VBox dialogPaneContent = new VBox(10);
        TextArea contentTextArea = new TextArea(selectedNotification.getFullContent());
        contentTextArea.setWrapText(true);
        contentTextArea.setEditable(false);
        contentTextArea.setPrefHeight(200);
        dialogPaneContent.getChildren().add(new Label("Loại: " + selectedNotification.getNotificationType()));
        dialogPaneContent.getChildren().add(new Label("Ngày gửi: " + selectedNotification.getSentAt().format(dateTimeFormatter)));
        dialogPaneContent.getChildren().add(contentTextArea);


        if (selectedNotification.getLinkUrl() != null && !selectedNotification.getLinkUrl().isEmpty()) {
            Hyperlink link = new Hyperlink("Link đính kèm: " + selectedNotification.getLinkUrl());
            link.setOnAction(e -> openLink(selectedNotification.getLinkUrl()));
            dialogPaneContent.getChildren().add(link);
        }

        alert.getDialogPane().setContent(dialogPaneContent);
        alert.showAndWait();

        // Bỏ chọn item để có thể click lại
        notificationsListView.getSelectionModel().clearSelection();
    }

    private void openLink(String url) {
        try {
            if (hostServices != null) { // Ưu tiên dùng HostServices của JavaFX
                hostServices.showDocument(url);
            } else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) { // Fallback
                Desktop.getDesktop().browse(new URI(url));
            } else {
                showError("Không thể mở link trên hệ thống này.");
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
            showError("Lỗi khi mở link: " + e.getMessage());
        }
    }


    private void updatePaginationControls(Page<?> pageData) {
        pageInfoLabel.setText("Trang " + (pageData.getNumber() + 1) + "/" + (pageData.getTotalPages() == 0 ? 1: pageData.getTotalPages()));
        prevPageButton.setDisable(pageData.isFirst());
        nextPageButton.setDisable(pageData.isLast());
    }

    @FXML
    private void handlePrevPage(ActionEvent event) {
        if (currentPage > 0) {
            currentPage--;
            loadNotifications();
        }
    }

    @FXML
    private void handleNextPage(ActionEvent event) {
        if ((currentPage + 1) * PAGE_SIZE < totalNotifications) {
            currentPage++;
            loadNotifications();
        }
    }

    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setTextFill(Color.GREEN);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setTextFill(Color.RED);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void clearStatusLabel() {
        statusLabel.setText("");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }

    // Lớp nội (inner class) cho custom ListCell
    private class NotificationListCell extends ListCell<ResidentNotificationViewDTO> {
        private final HBox hbox = new HBox(10);
        private final Circle unreadIndicator = new Circle(5, Color.DODGERBLUE);
        private final VBox contentVBox = new VBox(3);
        private final Label subjectLabel = new Label();
        private final Label snippetLabel = new Label();
        private final Label dateLabel = new Label();

        public NotificationListCell() {
            super();
            subjectLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
            snippetLabel.setTextFill(Color.GRAY);
            dateLabel.setFont(Font.font("System", 10));
            dateLabel.setTextFill(Color.DARKSLATEGRAY);

            HBox.setHgrow(contentVBox, Priority.ALWAYS); // Để contentVBox chiếm hết không gian còn lại
            contentVBox.getChildren().addAll(subjectLabel, snippetLabel);

            VBox rightPane = new VBox(dateLabel); // Để ngày và chỉ báo chưa đọc ở bên phải
            rightPane.setAlignment(Pos.TOP_RIGHT);

            hbox.getChildren().addAll(unreadIndicator, contentVBox, rightPane);
            hbox.setAlignment(Pos.CENTER_LEFT);
            setGraphic(hbox); // Phải setGraphic ở đây
            setPadding(new Insets(5, 10, 5, 10));
        }

        @Override
        protected void updateItem(ResidentNotificationViewDTO item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null); // Quan trọng: clear graphic khi cell rỗng
            } else {
                subjectLabel.setText(item.getEmailSubject());
                snippetLabel.setText(item.getContentSnippet() != null && item.getContentSnippet().length() > 80 ?
                        item.getContentSnippet().substring(0, 80) + "..." :
                        item.getContentSnippet());
                dateLabel.setText(item.getSentAt().format(dateTimeFormatter));
                unreadIndicator.setVisible(!item.isRead());
                setGraphic(hbox); // Đảm bảo graphic được hiển thị
            }
        }
    }
}