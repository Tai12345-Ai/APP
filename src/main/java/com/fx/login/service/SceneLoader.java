package com.fx.login.service;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.net.URL;

public class SceneLoader {

    private static ConfigurableApplicationContext springContext; // Thêm biến này

    public static void setSpringContext(ConfigurableApplicationContext context) { // Thêm phương thức này
        springContext = context;
    }

    private static FXMLLoader getLoader(String fxmlFile) {
        FXMLLoader loader = new FXMLLoader();
        URL xmlUrl = SceneLoader.class.getResource("/ui/" + fxmlFile);
        if (xmlUrl == null) {
            throw new RuntimeException("Cannot find FXML file: " + fxmlFile);
        }
        loader.setLocation(xmlUrl);

        // Nếu springContext đã được set, sử dụng nó để tạo controller
        if (springContext != null) {
            loader.setControllerFactory(springContext::getBean);
        }
        return loader;
    }




    // Dành cho sự kiện MouseEvent (nếu dùng click chuột)
    public static void loadMouseEvent(MouseEvent e, String fxmlFile) throws IOException {
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        Parent root = FXMLLoader.load(SceneLoader.class.getResource("/ui/" + fxmlFile));
        stage.setScene(new Scene(root));
        stage.show();
    }

    // Dành cho sự kiện ActionEvent (như click Button)
    public static void loadActionEvent(ActionEvent event, String fxmlFile) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Parent root = FXMLLoader.load(SceneLoader.class.getResource("/ui/" + fxmlFile));
        stage.setScene(new Scene(root));
        stage.show();
    }

    public static boolean checkLogin(String username, String password) {
        return username.equals("admin") && password.equals("123456");
    }
    // Load giao diện mới từ file FXML
    public static void loadScene(Stage stage, String fxmlFile) throws IOException {
        Parent root = FXMLLoader.load(SceneLoader.class.getResource("/ui/" + fxmlFile));
        stage.setScene(new Scene(root));
        stage.show();
    }
}
