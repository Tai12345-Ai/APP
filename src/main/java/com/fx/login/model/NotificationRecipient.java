package com.fx.login.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "notification_recipients")
@Data
@NoArgsConstructor
public class NotificationRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // Giữ LAZY, đã dùng fetch join ở repo
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(name = "sent_via_email")
    private boolean sentViaEmail;

    @Column(name = "sent_via_app")
    private boolean sentViaApp;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Transient
    public boolean isRead() {
        return readAt != null;
    }

    public NotificationRecipient(Notification notification, User recipient, boolean sentViaEmail, boolean sentViaApp) {
        this.notification = notification;
        this.recipient = recipient;
        this.sentViaEmail = sentViaEmail;
        this.sentViaApp = sentViaApp;
        this.sentAt = LocalDateTime.now();
    }

    public void markAsRead() {
        if (readAt == null) {
            readAt = LocalDateTime.now();
        }
    }
}