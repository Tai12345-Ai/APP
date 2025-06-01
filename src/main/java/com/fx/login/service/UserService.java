package com.fx.login.service;

import com.fx.login.model.User;
import com.fx.login.model.Resident;
import com.fx.login.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {
    @Autowired
    private ServiceUtil serviceUtil;

    // Helper method để lấy UserRepo (giả sử bạn đã có)
    private UserRepo getUserRepo() {
        return serviceUtil.getUserRepo();
    }

    // ====================== CRUD CƠ BẢN (GIỮ NGUYÊN) ======================
    public User save(User entity) {
        return serviceUtil.getUserRepo().save(entity);
    }

    public User update(User entity) {
        return serviceUtil.getUserRepo().save(entity);
    }

    public void delete(User entity) {
        serviceUtil.getUserRepo().delete(entity);
    }

    public void delete(Long id) {
        serviceUtil.getUserRepo().deleteById(id);
    }

    public Optional<User> find(Long id) {
        return serviceUtil.getUserRepo().findById(id);
    }

    public List<User> findAll() {
        return serviceUtil.getUserRepo().findAll();
    }

    // ====================== RESIDENT MANAGEMENT (GIỮ NGUYÊN) ======================
    public List<User> findResidentsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<User> usersFound = getUserRepo().findAllById(ids);
        return usersFound.stream()
                .filter(user -> user.getRole() == User.Role.Resident)
                .collect(Collectors.toList());
    }

    public List<User> findAllResidents() {
        return findAll().stream()
                .filter(user -> user.getRole() == User.Role.Resident)
                .collect(Collectors.toList());
    }

    public long getResidentCount() {
        return findAll().stream()
                .filter(user -> user.getRole() == User.Role.Resident)
                .count();
    }

    // ====================== AUTHENTICATION (GIỮ NGUYÊN + CẢI THIỆN) ======================
    public boolean authenticate(String username, String password) {
        Optional<User> user = this.findByEmail(username);
        if (user.isEmpty()) {
            return false;
        } else {
            User u = user.get();
            // ✅ CẢI THIỆN: Thêm null check
            return password != null && password.equals(u.getPassword());
        }
    }

    public Optional<User> findByEmail(String email) {
        return getUserRepo().findByEmail(email);
    }

    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    // ====================== BATCH OPERATIONS (GIỮ NGUYÊN) ======================
    public void deleteInBatch(List<User> users) {
        getUserRepo().deleteAllInBatch(users);
    }

    // ====================== ✅ THÊM MỚI CHO PAYMENT SYSTEM ======================

    /**
     * ✅ Tìm user theo resident
     */
    public Optional<User> findByResident(Resident resident) {
        if (resident == null) {
            return Optional.empty();
        }

        return getUserRepo().findAll().stream()
                .filter(user -> user.getResident() != null &&
                        user.getResident().getId().equals(resident.getId()))
                .findFirst();
    }

    /**
     * ✅ Lấy tất cả user resident (có liên kết với resident)
     */
    public List<User> findAllResidentsWithResidentEntity() {
        return getUserRepo().findAll().stream()
                .filter(user -> User.Role.Resident.equals(user.getRole()) &&
                        user.getResident() != null)
                .collect(Collectors.toList());
    }

    /**
     * ✅ Tìm user theo tên đầy đủ
     */
    public List<User> findByFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return getUserRepo().findAll().stream()
                .filter(user -> fullName.equals(user.getFullname()))
                .collect(Collectors.toList());
    }

    /**
     * ✅ Tìm user theo căn hộ (thông qua resident)
     */
    public List<User> findByApartmentNumber(String apartmentNumber) {
        if (apartmentNumber == null || apartmentNumber.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return getUserRepo().findAll().stream()
                .filter(user -> user.getResident() != null &&
                        apartmentNumber.equals(user.getResident().getApartmentNumber()))
                .collect(Collectors.toList());
    }

    /**
     * ✅ Kiểm tra user có phải resident không
     */
    public boolean isResident(Long userId) {
        Optional<User> userOpt = find(userId);
        return userOpt.isPresent() &&
                User.Role.Resident.equals(userOpt.get().getRole());
    }

    /**
     * ✅ Kiểm tra user có phải admin không
     */
    public boolean isAdmin(Long userId) {
        Optional<User> userOpt = find(userId);
        return userOpt.isPresent() &&
                User.Role.Admin.equals(userOpt.get().getRole());
    }

    /**
     * ✅ Liên kết user với resident
     */
    public User linkUserWithResident(Long userId, Resident resident) {
        Optional<User> userOpt = find(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy user với ID: " + userId);
        }

        User user = userOpt.get();
        user.setResident(resident);
        return save(user);
    }

    /**
     * ✅ Hủy liên kết user với resident
     */
    public User unlinkUserFromResident(Long userId) {
        Optional<User> userOpt = find(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy user với ID: " + userId);
        }

        User user = userOpt.get();
        user.setResident(null);
        return save(user);
    }

    /**
     * ✅ Tạo user mới với role
     */
    public User createUser(String fullName, String email, String password, User.Role role) {
        if (existsByEmail(email)) {
            throw new RuntimeException("Email đã tồn tại: " + email);
        }

        User newUser = new User();
        newUser.setFullname(fullName);
        newUser.setEmail(email);
        newUser.setPassword(password);
        newUser.setRole(role);
        newUser.setDatecreated(java.time.LocalDateTime.now());

        return save(newUser);
    }

    /**
     * ✅ Tạo user resident và liên kết với resident entity
     */
    public User createResidentUser(String fullName, String email, String password, Resident resident) {
        User user = createUser(fullName, email, password, User.Role.Resident);
        user.setResident(resident);
        return save(user);
    }

    /**
     * ✅ Đổi mật khẩu
     */
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        Optional<User> userOpt = find(userId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        if (!oldPassword.equals(user.getPassword())) {
            return false;
        }

        user.setPassword(newPassword);
        save(user);
        return true;
    }

    /**
     * ✅ Reset mật khẩu (chỉ admin)
     */
    public User resetPassword(Long userId, String newPassword) {
        Optional<User> userOpt = find(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy user với ID: " + userId);
        }

        User user = userOpt.get();
        user.setPassword(newPassword);
        return save(user);
    }

    /**
     * ✅ Lấy thống kê user
     */
    public UserStatistics getUserStatistics() {
        List<User> allUsers = findAll();

        long totalUsers = allUsers.size();
        long adminCount = allUsers.stream()
                .filter(user -> User.Role.Admin.equals(user.getRole()))
                .count();
        long residentCount = allUsers.stream()
                .filter(user -> User.Role.Resident.equals(user.getRole()))
                .count();
        long linkedResidentCount = allUsers.stream()
                .filter(user -> User.Role.Resident.equals(user.getRole()) && user.getResident() != null)
                .count();

        return new UserStatistics(totalUsers, adminCount, residentCount, linkedResidentCount);
    }

    // ====================== INNER CLASS FOR STATISTICS ======================
    public static class UserStatistics {
        private final long totalUsers;
        private final long adminCount;
        private final long residentCount;
        private final long linkedResidentCount;

        public UserStatistics(long totalUsers, long adminCount, long residentCount, long linkedResidentCount) {
            this.totalUsers = totalUsers;
            this.adminCount = adminCount;
            this.residentCount = residentCount;
            this.linkedResidentCount = linkedResidentCount;
        }

        // Getters
        public long getTotalUsers() { return totalUsers; }
        public long getAdminCount() { return adminCount; }
        public long getResidentCount() { return residentCount; }
        public long getLinkedResidentCount() { return linkedResidentCount; }
        public long getUnlinkedResidentCount() { return residentCount - linkedResidentCount; }

        @Override
        public String toString() {
            return String.format("UserStatistics{total=%d, admin=%d, resident=%d, linked=%d, unlinked=%d}",
                    totalUsers, adminCount, residentCount, linkedResidentCount, getUnlinkedResidentCount());
        }
    }
}