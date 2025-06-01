
    package com.fx.login.service;

    import com.fx.login.dto.NotificationHistoryViewDTO;
    import com.fx.login.dto.ResidentNotificationViewDTO;
    import com.fx.login.model.Notification;
    import com.fx.login.model.NotificationRecipient;
    import com.fx.login.model.NotificationType;
    import com.fx.login.model.User;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.Pageable;

    import java.time.LocalDate;
    import java.util.List;
    
    public interface NotificationService {
    
    // ====================== ADMIN FUNCTIONS ======================
    
    /**
     * Tạo và gửi thông báo 
     */
    Notification createAndSendNotification(User senderAdmin, List<User> recipients,
                                         String recipientTargetDescription, NotificationType notificationType,
                                         String subject, String content, String linkUrl,
                                         boolean sendViaEmail, boolean sendViaInApp);
    
    /**
     * Cho Admin xem lịch sử (có filter và phân trang)
     */
    Page<NotificationHistoryViewDTO> getNotificationHistoryForAdmin(
            String senderFilter, String recipientFilter, NotificationType typeFilter,
            String subjectFilter, LocalDate dateFrom, LocalDate dateTo,
            Boolean sentViaEmail, Boolean sentViaApp, Pageable pageable);
    
    // ====================== COMMON FUNCTIONS ======================
    
    /**
     * Lấy thông báo theo ID
     */
    Notification getNotificationById(Long id);
    
    // ====================== USER/RESIDENT FUNCTIONS ======================
    
    /**
     * Lấy danh sách thông báo của người dùng (theo đối tượng NotificationRecipient)
     * Sử dụng với cả User thông thường
     */
    Page<NotificationRecipient> getNotificationsForUser(User user, boolean unreadOnly, Pageable pageable);
    
    /**
     * Đánh dấu thông báo đã đọc cho user thông thường (theo notification ID)
     */
    NotificationRecipient markAsRead(Long notificationId, User user);
    
    /**
     * Đánh dấu tất cả thông báo đã đọc cho user thông thường
     */
    int markAllAsRead(User user);
    
    /**
     * Đếm số thông báo chưa đọc cho user thông thường
     */
    int countUnreadNotifications(User user);
    
    /**
     * Lấy danh sách thông báo cho resident (trả về DTO chuyên dụng)
     */
    Page<ResidentNotificationViewDTO> getNotificationsForResident(
            User resident, boolean unreadOnly, Pageable pageable);
    
    /**
     * Đánh dấu thông báo đã đọc cho resident (theo recipient ID)
     */
    boolean markAsRead(User resident, Long notificationRecipientId);
    
    /**
     * Đánh dấu tất cả thông báo chưa đọc là đã đọc cho resident
     */
    int markAllAsReadForResident(User resident);
    
    /**
     * Đếm số thông báo chưa đọc cho resident (để hiển thị badge)
     */
    long countUnreadNotificationsForResident(User resident);
}