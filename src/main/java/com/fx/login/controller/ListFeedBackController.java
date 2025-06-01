package com.fx.login.controller;

import com.fx.login.model.FeedBack;
import com.fx.login.MainApplication;
import com.fx.login.model.User;
import com.fx.login.service.FeedBackService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ListFeedBackController {

    @FXML
    private TextField searchField;

    @FXML
    private TableView<FeedBack> feedbackTable;

    @FXML
    private TableColumn<FeedBack, Number> colId;

    @FXML
    private TableColumn<FeedBack, String> colTitle;

    @FXML
    private TableColumn<FeedBack, String> colContent;

    @FXML
    private TableColumn<FeedBack, String> colCreatedAt;

    @FXML
    private TableColumn<FeedBack, String> colStatus;

    @FXML
    private TableColumn<FeedBack, String> colSender;

    @FXML
    private TableColumn<FeedBack, Void> colAction;

    private ObservableList<FeedBack> feedBacksList = FXCollections.observableArrayList();
    private FeedBackService feedBackService;

    private boolean saved = false;
    // Getter cho cờ 'saved'
    public boolean isSaved() {
        return saved;
    }

    private boolean showAllMode = false;

    public void setShowAllMode(boolean showAllMode) {
        this.showAllMode = showAllMode;
    }

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    public void setFeedBacksList(ObservableList<FeedBack> feedBacksUiList) {
        if (feedBacksUiList != null) {
            this.feedBacksList = feedBacksUiList;
        } else {
            this.feedBacksList = FXCollections.observableArrayList(); // Tránh null
        }
        feedbackTable.setItems(this.feedBacksList); // Gán dữ liệu cho bảng
        this.saved = false;
    }

    // Khởi tạo controller
    @FXML
    public void initialize() {
        // Lấy feeService từ Spring Context
        if (MainApplication.getSpringContext() != null) {
            this.feedBackService = MainApplication.getSpringContext().getBean(FeedBackService.class);
        } else {
            // Xử lý trường hợp context chưa sẵn sàng (ví dụ: hiển thị lỗi)
            System.err.println("SpringContext is null in FeedBackController.initialize()");
            showAlert("Lỗi hệ thống", "Không thể khởi tạo danh sách góp ý.");
            return; // Không thể tiếp tục nếu không có service
        }

        setupTableColumns();
        loadFeedBacksFromDb(); // Tải dữ liệu từ DB
    }

    private void setupTableColumns() {
        // Cột dữ liệu cơ bản
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        colTitle.setCellValueFactory(cellData -> cellData.getValue().titleProperty());
        colContent.setCellValueFactory(cellData -> cellData.getValue().contentProperty());
        colCreatedAt.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        ));
        colSender.setCellValueFactory(cellData -> cellData.getValue().senderProperty());

        // Cột trạng thái với ComboBox
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        colStatus.setCellFactory(col -> new TableCell<>() {
            private final ComboBox<String> comboBox = new ComboBox<>();

            {
                comboBox.getItems().addAll("Đã xử lý", "Chưa xử lý", "Đang xử lý");
                comboBox.setOnAction(event -> {
                    FeedBack fb = getTableView().getItems().get(getIndex());
                    fb.setStatus(comboBox.getValue());
                    // Cập nhật xuống DB nếu cần
                    feedBackService.updateFeedBack(fb.getId(), fb);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    comboBox.setValue(item);
                    setGraphic(comboBox);
                }
            }
        });

        // Cột hành động với 3 nút: Xem, Xóa, và Ẩn/Hiện
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnView = new Button("Xem");
            private final Button btnDelete = new Button("Xóa");
            private final Button btnToggle = new Button(); // Hiển thị Ẩn hoặc Hiện
            private final HBox actionBox = new HBox(5, btnView, btnDelete, btnToggle);

            {
                btnView.setOnAction(event -> {
                    FeedBack fb = getTableView().getItems().get(getIndex());
                    showDetailContent(fb, "Nội dung chi tiết góp ý"); // hoặc mở form riêng
                });

                btnDelete.setOnAction(event -> {
                    FeedBack fb = getTableView().getItems().get(getIndex());
                    // Xác nhận xóa
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Xóa góp ý này?", ButtonType.YES, ButtonType.NO);
                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.YES) {
                            feedBackService.delete(fb.getId());
                            feedBacksList.remove(fb);
                        }
                    });
                });

                btnToggle.setOnAction(event -> {
                    FeedBack fb = getTableView().getItems().get(getIndex());
                    if ("Ẩn".equals(btnToggle.getText())) {
                        btnToggle.setText("Hiện");
                        fb.setVisibleToUser(false); // cập nhật trạng thái hiển thị
                    } else {
                        btnToggle.setText("Ẩn");
                        fb.setVisibleToUser(true);
                    }
                    feedBackService.updateVisibility(fb.getId(), fb.isVisibleToUser());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    FeedBack fb = getTableView().getItems().get(getIndex());
                    btnToggle.setText(fb.isVisibleToUser() ? "Ẩn" : "Hiện");
                    setGraphic(actionBox);
                }
            }
        });

        feedbackTable.setItems(feedBacksList);
    }

    private User currentUser;

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    private void loadFeedBacksFromDb() {
        if (feedBackService == null) return;

        try {
            List<FeedBack> allFeedBacks = feedBackService.findAll();
            allFeedBacks.forEach(FeedBack::syncProperties);

            if (showAllMode) {
                // Xem toàn bộ góp ý
                this.feedBacksList.setAll(allFeedBacks);
            } else if (currentUser != null) {
                // Lọc góp ý của riêng người dùng
                List<FeedBack> filtered = allFeedBacks.stream()
                        .filter(fb -> fb.getSender().equals(currentUser.getFullname()))
                        .collect(Collectors.toList());
                this.feedBacksList.setAll(filtered);
            } else {
                this.feedBacksList.clear(); // Không có user và không ở chế độ showAll → không hiển thị gì
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi", "Không thể tải danh sách góp ý: " + e.getMessage());
        }
    }

    // Xử lý tìm kiếm góp ý
    @FXML
    private void onSearch() {
        String keyword = searchField.getText().trim().toLowerCase();
        search1(keyword,"");
    }

    private void search1(String keyword1, String keyword2) {
        if ((keyword1 == null || keyword1.isEmpty()) && (keyword2 == null || keyword2.isEmpty())) {
            loadFeedBacksFromDb(); // Tải lại toàn bộ danh sách nếu cả hai keyword rỗng
            return;
        }

        // Lọc danh sách hiển thị
        ObservableList<FeedBack> filteredList = feedBacksList.stream()
                .filter(feedBack -> {
                    boolean match1 = matchesKeyword1(feedBack, keyword1);
                    boolean match2 = matchesKeyword2(feedBack, keyword2);
                    return match1 && match2;
                })
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        feedbackTable.setItems(filteredList);
    }

    // Hàm hỗ trợ kiểm tra một keyword có khớp với nút hành động không
    private boolean matchesKeyword2(FeedBack feedBack, String keyword) {
        if (keyword == null || keyword.isEmpty()) return true; // Nếu rỗng thì xem như luôn khớp

        keyword = keyword.toLowerCase();
        try {
            if (feedBack != null && String.valueOf(feedBack.isVisibleToUser()).toLowerCase().contains(keyword)) return true;
        } catch (NumberFormatException ignored) {}

        return false;
    }

    // Hàm hỗ trợ kiểm tra một keyword có khớp với entity không
    private boolean matchesKeyword1(FeedBack feedBack, String keyword) {
        if (keyword == null || keyword.isEmpty()) return true; // Nếu rỗng thì xem như luôn khớp

        keyword = keyword.toLowerCase();
        try {
            Long id = Long.parseLong(keyword);
            if (feedBack.getId() != null && feedBack.getId().equals(id)) return true;
        } catch (NumberFormatException ignored) {}

        return (feedBack.getTitle() != null && feedBack.getTitle().toLowerCase().contains(keyword)) ||
                (feedBack.getContent() != null && feedBack.getContent().toLowerCase().contains(keyword)) ||
                (feedBack.getCreatedAt() != null && feedBack.getCreatedAt().toString().contains(keyword)) ||
                (feedBack.getStatus() != null && feedBack.getStatus().toLowerCase().contains(keyword)) ||
                (feedBack.getSender() != null && feedBack.getSender().toLowerCase().contains(keyword));
    }

    private boolean showDetailContent(FeedBack feedBack, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/detailfeedback.fxml"));
            Parent root = loader.load();
            DetailFeedBackController controller = loader.getController();
            controller.setFeedBack(feedBack); // Truyền đối tượng feedBack vào form
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.initModality(Modality.APPLICATION_MODAL); // Chặn tương tác với cửa sổ khác
            stage.setScene(new Scene(root));
            // controller.setStage(stage); // Nếu form controller cần tự đóng

            stage.showAndWait(); // Chờ cho đến khi form đóng

            return controller.isSaved(); // Trả về trạng thái lưu từ form controller

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi giao diện", "Không thể mở bản chi tiết của góp ý.");
            return false;
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

}
