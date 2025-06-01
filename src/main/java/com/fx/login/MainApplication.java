package com.fx.login;

import com.fx.login.controller.LoginController;
import com.fx.login.controller.SignupController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class MainApplication extends Application {

    private static ConfigurableApplicationContext applicationContext;

    public static ConfigurableApplicationContext getSpringContext() {
        return applicationContext;
    }

    @Override
    public void init() {
        String[] args = getParameters().getRaw().toArray(new String[0]);
        this.applicationContext = new SpringApplicationBuilder()
                .sources(App.class)
                .web(WebApplicationType.NONE) // 👈 thêm dòng này để ngăn Spring khởi động web server
                .run(args);
    }

    @Override
    public void start(Stage stage) {
        FxWeaver fxWeaver = applicationContext.getBean(FxWeaver.class);
        Parent root = fxWeaver.loadView(LoginController.class);
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Phần mềm quản lý chung cư");
        stage.setResizable(false);
        stage.show();

    }

    @Override
    public void stop() {
        this.applicationContext.close();
        Platform.exit();
    }

}
