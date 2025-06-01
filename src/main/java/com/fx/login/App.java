package com.fx.login;


import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling

public class App {

    public static void main(String[] args) {
        Application.launch(MainApplication.class, args);
    }

}
