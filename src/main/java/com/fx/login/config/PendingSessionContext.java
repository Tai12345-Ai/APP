package com.fx.login.config;

import com.fx.login.model.PendingUser;
import com.fx.login.model.User;

public class PendingSessionContext {
    private static PendingSessionContext instance;
    private PendingUser currentUser;

    private PendingSessionContext() {}

    public static PendingSessionContext getInstance() {
        if (instance == null) {
            instance = new PendingSessionContext();
        }
        return instance;
    }

    public PendingUser getCurrentPendingUser() {
        return currentUser;
    }

    public void setCurrentPendingUser(PendingUser currentUser) {
        this.currentUser = currentUser;
    }
}
