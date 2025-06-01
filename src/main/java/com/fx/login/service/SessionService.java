package com.fx.login.service;

import com.fx.login.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    @Autowired
    private UserService userService;

    private static User currentUser;

    // Đăng nhập
    public boolean login(String email, String password) {
        if (userService.authenticate(email, password)) {
            currentUser = userService.findByEmail(email).orElse(null);
            return true;
        }
        return false;
    }

    // Lấy user hiện tại
    public User getCurrentUser() {
        return currentUser;
    }

    // Kiểm tra quyền
    public boolean isAdmin() {
        return currentUser != null && User.Role.Admin.equals(currentUser.getRole());
    }

    public boolean isResident() {
        return currentUser != null && User.Role.Resident.equals(currentUser.getRole());
    }

    // Đăng xuất
    public void logout() {
        currentUser = null;
    }

    // Kiểm tra đăng nhập
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    // Lấy tên hiển thị
    public String getCurrentUserDisplayName() {
        return currentUser != null ? currentUser.getFullname() : "Guest";
    }
}