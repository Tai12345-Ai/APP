package com.fx.login.controller;

import com.fx.login.config.Router;
import com.fx.login.config.SessionContext;
import com.fx.login.dto.NotificationHistoryViewDTO;
import com.fx.login.model.Notification;
import com.fx.login.model.NotificationType;
import com.fx.login.model.User;
import com.fx.login.service.NotificationService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

@Component
@FxmlView("/ui/AdminNotificationHistoryView.fxml")
public class AdminNotificationHistoryController implements Initializable {

    @FXML private VBox historyRootPane;
    @FXML private TextField senderFilterField;
    @FXML private TextField recipientFilterField;
    @FXML private ComboBox<NotificationType> typeFilterComboBox;
    @FXML private ComboBox<ChannelFilter> channelFilterComboBox;
    @FXML private TextField subjectFilterField;
    @FXML private DatePicker dateFromFilterPicker;
    @FXML private DatePicker dateToFilterPicker;
    @FXML private TableView<NotificationHistoryViewDTO> notificationsTableView;
    @FXML private TableColumn<NotificationHistoryViewDTO, Long> notificationIdColumn;
    @FXML private TableColumn<NotificationHistoryViewDTO, String> sentAtColumn;
    @FXML private TableColumn<NotificationHistoryViewDTO, String> senderAdminColumn;
    @FXML private TableColumn<NotificationHistoryViewDTO, String> recipientColumn;
    @FXML private TableColumn<NotificationHistoryViewDTO, String> typeColumn;
    @FXML private TableColumn<NotificationHistoryViewDTO, String> subjectColumn;
    @FXML private TableColumn<NotificationHistoryViewDTO, String> contentSnippetColumn;
    @FXML private TableColumn<NotificationHistoryViewDTO, String> sendChannelsColumn;
    @FXML private TableColumn<NotificationHistoryViewDTO, String> linkColumn;
    @FXML private Label pageInfoLabel;
    @FXML private Button prevPageButton;
    @FXML private Button nextPageButton;
    @FXML private Label totalItemsLabel;
    @FXML private Label statusLabel;

    @Autowired
    private Router router;
    @Autowired
    private NotificationService notificationService;

    private User currentAdmin;
    private final ObservableList<NotificationHistoryViewDTO> notificationList = FXCollections.observableArrayList();
    private static final int PAGE_SIZE = 15;
    private int currentPage = 0;
    private long totalNotifications = 0;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // Enum để lọc theo kênh gửi thông báo
    private enum ChannelFilter {
        ALL("Tất cả"),
        EMAIL("Email"),
        INAPP("Hệ thống"),
        BOTH("Cả hai");

        private final String displayName;

        ChannelFilter(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            currentAdmin = SessionContext.getInstance().getCurrentUser();
            if (currentAdmin == null || currentAdmin.getRole() != User.Role.Admin) {
                if (statusLabel != null) {
                    statusLabel.setText("Lỗi: Bạn không có quyền truy cập.");
                    statusLabel.setTextFill(Color.RED);
                    statusLabel.setVisible(true);
                    statusLabel.setManaged(true);
                }
                if (notificationsTableView != null) notificationsTableView.setDisable(true);
                return;
            }

            setupTypeFilterComboBox();
            setupChannelFilterComboBox();
            setupTableColumns();

            if (notificationsTableView != null) {
                notificationsTableView.setItems(notificationList);

                // Double-click để xem chi tiết
                notificationsTableView.setRowFactory(tv -> {
                    TableRow<NotificationHistoryViewDTO> row = new TableRow<>();
                    row.setOnMouseClicked(event -> {
                        if (event.getClickCount() == 2 && !row.isEmpty()) {
                            handleViewNotificationDetail(new ActionEvent());
                        }
                    });
                    return row;
                });
            }

            loadNotificationHistory();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Lỗi khởi tạo: " + e.getMessage());
        }
    }

    private void setupTypeFilterComboBox() {
        if (typeFilterComboBox != null) {
            ObservableList<NotificationType> types = FXCollections.observableArrayList(NotificationType.values());
            types.add(0, null);
            typeFilterComboBox.setItems(types);
            typeFilterComboBox.setConverter(new javafx.util.StringConverter<NotificationType>() {
                @Override
                public String toString(NotificationType object) {
                    return object == null ? "Tất cả loại" : object.getDisplayName();
                }

                @Override
                public NotificationType fromString(String string) {
                    return null;
                }
            });
            typeFilterComboBox.getSelectionModel().selectFirst();
        }
    }

    private void setupChannelFilterComboBox() {
        if (channelFilterComboBox != null) {
            channelFilterComboBox.setItems(FXCollections.observableArrayList(ChannelFilter.values()));
            channelFilterComboBox.getSelectionModel().select(ChannelFilter.ALL);
        }
    }

    private void setupTableColumns() {
        if (notificationIdColumn == null) return;

        notificationIdColumn.setCellValueFactory(new PropertyValueFactory<>("notificationId"));

        sentAtColumn.setCellValueFactory(cellData -> {
            LocalDateTime sentAt = cellData.getValue().getSentAt();
            return new SimpleStringProperty(sentAt != null ? sentAt.format(dateTimeFormatter) : "");
        });

        senderAdminColumn.setCellValueFactory(new PropertyValueFactory<>("senderAdminInfo"));
        recipientColumn.setCellValueFactory(new PropertyValueFactory<>("recipientTargetDescription"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("notificationType"));
        subjectColumn.setCellValueFactory(new PropertyValueFactory<>("emailSubject"));

        contentSnippetColumn.setCellValueFactory(cellData -> {
            String content = cellData.getValue().getContentSnippet();
            if (content != null && content.length() > 50) {
                content = content.substring(0, 47) + "...";
            }
            return new SimpleStringProperty(content);
        });

        sendChannelsColumn.setCellValueFactory(cellData -> {
            NotificationHistoryViewDTO dto = cellData.getValue();
            StringBuilder channels = new StringBuilder();

            if (dto.isSentViaApp()) {
                channels.append("Hệ thống");
            }

            if (dto.isSentViaEmail()) {
                if (channels.length() > 0) {
                    channels.append(", ");
                }
                channels.append("Email");
            }

            if (channels.length() == 0) {
                channels.append("Không có");
            }

            return new SimpleStringProperty(channels.toString());
        });

        linkColumn.setCellValueFactory(cellData -> {
            String link = cellData.getValue().getLinkUrl();
            if (link != null && link.length() > 30) {
                link = link.substring(0, 27) + "...";
            }
            return new SimpleStringProperty(link);
        });

        sentAtColumn.setSortType(TableColumn.SortType.DESCENDING);
        notificationsTableView.getSortOrder().add(sentAtColumn);
    }

    @FXML
    private void handleGoBack(ActionEvent event) {
        try {
            // Lấy node nguồn (nút vừa click)
            Node source = (Node) event.getSource();
            Scene scene = source.getScene();

            // Tìm ScrollPane của dashboard theo fx:id
            ScrollPane scrollPane = (ScrollPane) scene.lookup("#scrollPane");
            if (scrollPane == null) {
                showError("Không tìm thấy dashboard. Vui lòng quay lại từ menu chính.");
                return;
            }

            // Load lại màn gửi thông báo qua Spring context
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/AdminSendNotificationView.fxml"));
            loader.setControllerFactory(com.fx.login.MainApplication.getSpringContext()::getBean);
            Parent sendNotificationView = loader.load();

            // Set vào content của scrollPane (phần bên phải dashboard)
            scrollPane.setContent(sendNotificationView);

        } catch (IOException e) {
            e.printStackTrace();
            showError("Lỗi khi quay lại: " + e.getMessage());
        }
    }

    @FXML
    private void handleApplyFilters(ActionEvent event) {
        currentPage = 0;
        loadNotificationHistory();
    }

    @FXML
    private void handleRefreshData(ActionEvent event) {
        loadNotificationHistory();
    }

    @FXML
    private void handleViewNotificationDetail(ActionEvent event) {
        NotificationHistoryViewDTO selected = notificationsTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Vui lòng chọn một thông báo để xem chi tiết.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/NotificationDetailView.fxml"));
            Parent root = loader.load();

            NotificationDetailController controller = loader.getController();
            controller.setNotificationId(selected.getNotificationId());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Chi tiết thông báo");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // Refresh sau khi xem chi tiết (có thể đã cập nhật)
            loadNotificationHistory();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Lỗi khi mở chi tiết thông báo: " + e.getMessage());
        }
    }

    @FXML
    private void handleResendNotification(ActionEvent event) {
        NotificationHistoryViewDTO selected = notificationsTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Vui lòng chọn một thông báo để gửi lại.");
            return;
        }

        // Xác nhận gửi lại
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận gửi lại thông báo");
        alert.setHeaderText("Gửi lại thông báo");
        alert.setContentText("Bạn có chắc chắn muốn gửi lại thông báo này?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Lấy thông báo gốc
                Notification original = notificationService.getNotificationById(selected.getNotificationId());
                if (original == null) {
                    showError("Không tìm thấy thông tin thông báo gốc.");
                    return;
                }

                // Mở form gửi thông báo với dữ liệu có sẵn
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/AdminSendNotificationView.fxml"));
                Parent root = loader.load();

                AdminSendNotificationController controller = loader.getController();
                controller.prepopulateFromExisting(original);

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (Exception e) {
                e.printStackTrace();
                showError("Lỗi khi gửi lại thông báo: " + e.getMessage());
            }
        }
    }

    private void loadNotificationHistory() {
        clearStatusLabel();
        try {
            Pageable pageable = PageRequest.of(currentPage, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "sentAt"));

            String senderFilter = (senderFilterField != null) ? senderFilterField.getText().trim() : "";
            String recipientFilter = (recipientFilterField != null) ? recipientFilterField.getText().trim() : "";
            NotificationType typeFilter = (typeFilterComboBox != null) ? typeFilterComboBox.getValue() : null;
            String subjectFilter = (subjectFilterField != null) ? subjectFilterField.getText().trim() : "";
            LocalDate dateFrom = (dateFromFilterPicker != null) ? dateFromFilterPicker.getValue() : null;
            LocalDate dateTo = (dateToFilterPicker != null) ? dateToFilterPicker.getValue() : null;

            // Process channel filter
            Boolean sentViaEmail = null;
            Boolean sentViaApp = null;

            if (channelFilterComboBox != null) {
                ChannelFilter channelFilter = channelFilterComboBox.getValue();
                if (channelFilter != null && channelFilter != ChannelFilter.ALL) {
                    switch (channelFilter) {
                        case EMAIL:
                            sentViaEmail = true;
                            sentViaApp = false;
                            break;
                        case INAPP:
                            sentViaEmail = false;
                            sentViaApp = true;
                            break;
                        case BOTH:
                            sentViaEmail = true;
                            sentViaApp = true;
                            break;
                    }
                }
            }

            Page<NotificationHistoryViewDTO> notificationPage = notificationService.getNotificationHistoryForAdmin(
                    senderFilter, recipientFilter, typeFilter, subjectFilter,
                    dateFrom, dateTo, sentViaEmail, sentViaApp, pageable
            );

            if (notificationList != null) {
                notificationList.setAll(notificationPage.getContent());
            }

            totalNotifications = notificationPage.getTotalElements();
            updatePaginationControls(notificationPage);

            if (notificationsTableView != null && notificationList.isEmpty()) {
                notificationsTableView.setPlaceholder(new Label("Không có dữ liệu lịch sử nào khớp với bộ lọc."));
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Lỗi khi tải lịch sử: " + e.getMessage());
            if (notificationList != null) {
                notificationList.clear();
            }
            if (notificationsTableView != null) {
                notificationsTableView.setPlaceholder(new Label("Lỗi khi tải dữ liệu."));
            }
        }
    }

    private void updatePaginationControls(Page<?> pageData) {
        if (totalItemsLabel == null || pageInfoLabel == null || prevPageButton == null || nextPageButton == null) return;

        totalItemsLabel.setText("Tổng số: " + pageData.getTotalElements());
        pageInfoLabel.setText("Trang " + (pageData.getNumber() + 1) + "/" +
                (pageData.getTotalPages() == 0 ? 1 : pageData.getTotalPages()));
        prevPageButton.setDisable(pageData.isFirst());
        nextPageButton.setDisable(pageData.isLast());
    }

    @FXML
    private void handlePrevPage(ActionEvent event) {
        if (currentPage > 0) {
            currentPage--;
            loadNotificationHistory();
        }
    }

    @FXML
    private void handleNextPage(ActionEvent event) {
        if ((currentPage + 1) * PAGE_SIZE < totalNotifications) {
            currentPage++;
            loadNotificationHistory();
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
}