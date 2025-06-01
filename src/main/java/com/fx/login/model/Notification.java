package com.fx.login.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_admin_id", nullable = false)
    private User senderAdmin; // Admin gửi thông báo

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType notificationType;

    @Column(nullable = false, length = 255)
    private String emailSubject;

    @Lob // For potentially long content
    @Column(nullable = false, columnDefinition = "TEXT")
    private String messageContent;

    @Column(length = 512) // Độ dài URL có thể lớn
    private String linkUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;

    // Trường này mô tả người nhận ban đầu (vd: "ALL", "1,2,3")
    // Hữu ích cho việc admin xem lại mục tiêu ban đầu của thông báo
    @Column(length = 255)
    private String recipientTargetDescription;

    // Quan hệ một-nhiều với NotificationRecipient
    // cascade = CascadeType.ALL: Khi lưu/xóa Notification, các NotificationRecipient liên quan cũng được xử lý
    // orphanRemoval = true: Nếu một NotificationRecipient bị xóa khỏi collection này, nó sẽ bị xóa khỏi DB
    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<NotificationRecipient> recipients = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }

    // Constructor tiện lợi
    public Notification(User senderAdmin, NotificationType notificationType, String emailSubject,
                        String messageContent, String linkUrl, String recipientTargetDescription) {
        this.senderAdmin = senderAdmin;
        this.notificationType = notificationType;
        this.emailSubject = emailSubject;
        this.messageContent = messageContent;
        this.linkUrl = linkUrl;
        this.recipientTargetDescription = recipientTargetDescription;
        this.sentAt = LocalDateTime.now();
    }

    // Helper method để thêm người nhận
    public void addRecipient(NotificationRecipient recipient) {
        recipients.add(recipient);
        recipient.setNotification(this);
    }

    public void removeRecipient(NotificationRecipient recipient) {
        recipients.remove(recipient);
        recipient.setNotification(null);
    }
}