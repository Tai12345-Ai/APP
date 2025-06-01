package com.fx.login.controller;

import com.fx.login.model.FeedBack;
import com.fx.login.MainApplication;
import com.fx.login.config.SessionContext;
import com.fx.login.model.User;
import com.fx.login.service.FeedBackService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
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

public class FeedBackController {

    //Tạo giả lập User
//    private User fakeUser;
//
//    {
//        fakeUser = new User();
//        fakeUser.setFullName("Nguyen Van A");
//        fakeUser.setEmail("nguyenvana@example.com");
//    }

    @FXML
    private TextField searchField;

    @FXML
    private Button btnSearch;

    @FXML
    private Button btnShowAll;

    @FXML
    private Button btnUserHistory;

    @FXML
    private Button btnAddFeedback;

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

    private final ObservableList<FeedBack> feedBacksUiList = FXCollections.observableArrayList();
    private FeedBackService feedBackService;

    // Khởi tạo controller
    @FXML
    User currentUser = SessionContext.getInstance().getCurrentUser();
    public void initialize() {
        // Lấy FeeService từ Spring Context
        if (MainApplication.getSpringContext() != null) {
            this.feedBackService = MainApplication.getSpringContext().getBean(FeedBackService.class);
        } else {
            // Xử lý trường hợp context chưa sẵn sàng (ví dụ: hiển thị lỗi)
            System.err.println("SpringContext is null in FeeController.initialize()");
            showAlert("Lỗi hệ thống", "Không thể khởi tạo trang đóng góp ý kiến.");
            return; // Không thể tiếp tục nếu không có service
        }

        setupTableColumns();
        loadFeedBacksFromDb(); // Tải dữ liệu từ DB
        search1("", "true");
        if (currentUser.getRole() == User.Role.Resident) colSender.setVisible(false);
        feedbackTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupTableColumns() {
        // Cột dữ liệu cơ bản
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        colId.setCellFactory(column -> new TableCell<FeedBack, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                    setAlignment(Pos.CENTER); // ✅ Căn giữa
                }
            }
        });
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
                    feedBackService.updateFeedBack(fb.getId(), fb); // cập nhật DB
                });
            }


            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    comboBox.setValue(item); // giờ item sẽ luôn có giá trị đúng
                    if (currentUser.getRole() == User.Role.Resident) comboBox.setDisable(true);
                    setGraphic(comboBox);
                }
            }

        });


        // Cột hành động với 3 nút: Xem, Xóa, và Ẩn/Hiện (X/Y)
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnView = new Button("Xem");
            private final Button btnDelete = new Button("Xóa");
            private final Button btnToggle = new Button(); // Hiển thị X hoặc Y
            private final HBox actionBox = new HBox(5,  btnDelete, btnView, btnToggle);

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
                            feedBacksUiList.remove(fb);
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
                    //reloadFilteredList(); // gọi lại hàm lọc để ẩn hiện dòng
                    search1("","true");
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    FeedBack fb = getTableView().getItems().get(getIndex());
                    if (currentUser.getRole() == User.Role.Resident)
                    {
                        btnDelete.setDisable(true);
                        btnDelete.setVisible(false);
                        btnToggle.setDisable(true);
                        btnToggle.setVisible(false);
                    }
                    btnToggle.setText(fb.isVisibleToUser() ? "Ẩn" : "Hiện");
                    setGraphic(actionBox);
                }
            }
        });

        feedbackTable.setItems(feedBacksUiList);
    }

    // Xử lý tìm kiếm góp ý
    @FXML
    private void onSearch() {
        String keyword = searchField.getText().trim().toLowerCase();
        search1(keyword,"true");
    }

    private void search1(String keyword1, String keyword2) {
        if ((keyword1 == null || keyword1.isEmpty()) && (keyword2 == null || keyword2.isEmpty())) {
            loadFeedBacksFromDb(); // Tải lại toàn bộ danh sách nếu cả hai keyword rỗng
            return;
        }

        // Lọc danh sách hiển thị
        ObservableList<FeedBack> filteredList = feedBacksUiList.stream()
                .filter(feedBack -> {
                    boolean match1 = matchesKeyword1(feedBack, keyword1);
                    boolean match2 = matchesKeyword2(feedBack, keyword2);
                    return match1 && match2;
                })
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        feedbackTable.setItems(filteredList);
    }

    private void search2(String keyword1, String keyword3) {
        if ((keyword1 == null || keyword1.isEmpty()) && (keyword3 == null || keyword3.isEmpty())) {
            loadFeedBacksFromDb(); // Tải lại toàn bộ danh sách nếu cả hai keyword rỗng
            return;
        }

        // Lọc danh sách hiển thị
        ObservableList<FeedBack> filteredList = feedBacksUiList.stream()
                .filter(feedBack -> {
                    boolean match1 = matchesKeyword1(feedBack, keyword1);
                    boolean match2 = matchesKeyword3(feedBack, keyword3);
                    return match1 && match2;
                })
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        feedbackTable.setItems(filteredList);
    }



    // Hàm hỗ trợ kiểm tra một keyword có khớp với tên người gửi không
    private boolean matchesKeyword3(FeedBack feedBack, String keyword) {
        if (keyword == null || keyword.isEmpty()) return true; // Nếu rỗng thì xem như luôn khớp

        keyword = keyword.toLowerCase();
        try {
            return (feedBack.getSender() != null && feedBack.getSender().toLowerCase().contains(keyword));
        } catch (NumberFormatException ignored) {}

        return false;
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

    // Hiển thị toàn bộ góp ý
    @FXML
    private void onShowAllFeedback() {
        showFeedBackList(feedBacksUiList, "Danh sách góp ý", true);
        search1("", "true");
    }

    // Xem lịch sử góp ý của người dùng hiện tại
    @FXML
    private void onViewUserHistory() {
//        SessionContext.getInstance().setCurrentUser(fakeUser);
        User currentUser = SessionContext.getInstance().getCurrentUser();
        String senderKeyword = currentUser.getFullname();
        ObservableList<FeedBack> filteredList = feedBacksUiList.stream()
                .filter(feedBack -> feedBack.getSender() != null && feedBack.getSender().toLowerCase().contains(senderKeyword.toLowerCase()))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        showFeedBackList(filteredList, "Danh sách góp ý của bạn", false);
        search1("", "true");
    }

    // Mở form thêm góp ý mới
    @FXML
    private void onAddFeedBack() {
        FeedBack newFeedBack = new FeedBack(); // Tạo đối tượng trống cho form
        boolean saved = showFeedBackForm(newFeedBack, "Thêm góp ý mới");

        if (saved) {
            try {
                // Không cần setId và ngày gửi, DB sẽ tự tạo
                FeedBack savedFee = feedBackService.createFeedBack(newFeedBack);
                savedFee.syncProperties(); // Đồng bộ properties (bao gồm ID và ngày gửi mới)
                feedBacksUiList.add(savedFee);
                feedbackTable.getSelectionModel().select(savedFee);
                search1("", "true");
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Lỗi lưu trữ", "Không thể thêm góp ý: " + e.getMessage());
            }
        }
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

    private boolean showFeedBackForm(FeedBack feedBack, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/addfeedback.fxml"));
            Parent root = loader.load();
            AddFeedBackController controller = loader.getController();
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

    private boolean showFeedBackList(ObservableList<FeedBack> feedBacksUiList, String title, Boolean mode) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/listfeedback.fxml"));
            Parent root = loader.load();
            ListFeedBackController controller = loader.getController();
            controller.setShowAllMode(mode);
            controller.setCurrentUser(SessionContext.getInstance().getCurrentUser());
            controller.setFeedBacksList(feedBacksUiList); // Truyền đối tượng feedbacksUilist vào form
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.initModality(Modality.APPLICATION_MODAL); // Chặn tương tác với cửa sổ khác
            stage.setScene(new Scene(root));
            // controller.setStage(stage); // Nếu form controller cần tự đóng

            stage.showAndWait(); // Chờ cho đến khi form đóng

            return controller.isSaved(); // Trả về trạng thái lưu từ form controller

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi giao diện", "Không thể mở form thông tin khoản thu.");
            return false;
        }
    }

    // Cập nhật bảng góp ý từ cơ sở dữ liệu
    private void loadFeedBacksFromDb() {
        if (feedBackService == null) return; // Service chưa được inject/lấy
        try {
            List<FeedBack> dbFeedBack = feedBackService.findAll();
            dbFeedBack.forEach(FeedBack::syncProperties); // QUAN TRỌNG: Đồng bộ properties sau khi load
            feedBacksUiList.setAll(dbFeedBack);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi tải dữ liệu", "Không thể tải danh sách góp ý từ cơ sở dữ liệu: " + e.getMessage());
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