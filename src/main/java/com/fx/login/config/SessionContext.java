package com.fx.login.config;

import com.fx.login.model.User;

public class SessionContext {   // Tạo Session
    private static SessionContext instance;
    private User currentUser;

    private SessionContext() {}

    public static SessionContext getInstance() {
        if (instance == null) {
            instance = new SessionContext();
        }
        return instance;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

}
