package com.fx.login.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResidentNotificationViewDTO {
    // Các thuộc tính cơ bản
    private Long id;                        // ID của recipient
    private Long notificationId;            // ID của thông báo gốc
    private String notificationType;        // Loại thông báo (hiển thị tên)
    private String emailSubject;            // Tiêu đề thông báo
    private String contentSnippet;          // Phần tóm tắt nội dung
    private String fullContent;             // Nội dung đầy đủ
    private LocalDateTime sentAt;           // Thời gian gửi
    private boolean read;                   // Đã đọc chưa
    private LocalDateTime readAt;           // Thời gian đọc (nếu đã đọc)
    private String linkUrl;                 // URL đính kèm (nếu có)
    private String senderName;              // Tên admin gửi
    private boolean sentViaEmail;           // Gửi qua email
    private boolean sentViaApp;             // Gửi qua ứng dụng

    // Getter thêm để hỗ trợ tương thích với controller
    public Long getNotificationRecipientId() {
        return id;
    }

    // Các phương thức hỗ trợ cho JavaFX
    public void updateProperties() {
        // Phương thức này có thể để trống vì chúng ta dùng Lombok @Data
        // Lombok sẽ tự động tạo các getter/setter cần thiết
    }

    // Alias cho các phương thức (để tương thích với code cũ)
    public void setSubject(String subject) {
        this.emailSubject = subject;
    }

    public void setContent(String content) {
        this.fullContent = content;
        // Tạo snippet từ nội dung
        if (content != null) {
            if (content.length() > 100) {
                this.contentSnippet = content.substring(0, 97) + "...";
            } else {
                this.contentSnippet = content;
            }
        }
    }

    public void setLink(String link) {
        this.linkUrl = link;
    }
}