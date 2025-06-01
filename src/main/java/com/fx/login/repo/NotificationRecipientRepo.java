package com.fx.login.repo;

import com.fx.login.model.NotificationRecipient;
import com.fx.login.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRecipientRepo extends JpaRepository<NotificationRecipient, Long> {
    @Query(
            value = "SELECT nr FROM NotificationRecipient nr " +
                    "JOIN FETCH nr.notification n " +
                    "JOIN FETCH n.senderAdmin " +
                    "WHERE nr.recipient = :recipient AND nr.readAt IS NULL " +
                    "ORDER BY n.sentAt DESC",
            countQuery = "SELECT COUNT(nr) FROM NotificationRecipient nr WHERE nr.recipient = :recipient AND nr.readAt IS NULL"
    )
    Page<NotificationRecipient> findByRecipientAndReadAtIsNullFetchNotification(@Param("recipient") User recipient, Pageable pageable);

    @Query(
            value = "SELECT nr FROM NotificationRecipient nr " +
                    "JOIN FETCH nr.notification n " +
                    "JOIN FETCH n.senderAdmin " +
                    "WHERE nr.recipient = :recipient " +
                    "ORDER BY n.sentAt DESC",
            countQuery = "SELECT COUNT(nr) FROM NotificationRecipient nr WHERE nr.recipient = :recipient"
    )
    Page<NotificationRecipient> findByRecipientFetchNotification(@Param("recipient") User recipient, Pageable pageable);

    // Nếu cần lấy thêm recipient.getFullname(), fetch luôn recipient:
    // @Query(
    //     value = "SELECT nr FROM NotificationRecipient nr JOIN FETCH nr.notification n JOIN FETCH n.senderAdmin JOIN FETCH nr.recipient WHERE nr.recipient = :recipient ...",
    //     countQuery = "SELECT COUNT(nr) FROM NotificationRecipient nr WHERE nr.recipient = :recipient ..."
    // )
    // Tìm theo người nhận và sắp xếp theo thời gian gửi giảm dần
    Page<NotificationRecipient> findByRecipientOrderBySentAtDesc(User recipient, Pageable pageable);

    // Tìm theo người nhận và chưa đọc, sắp xếp theo thời gian gửi giảm dần
    Page<NotificationRecipient> findByRecipientAndReadAtIsNullOrderBySentAtDesc(User recipient, Pageable pageable);

    // Tìm theo người nhận và chưa đọc (không phân trang)
    List<NotificationRecipient> findByRecipientAndReadAtIsNull(User recipient);

    // Đếm số thông báo chưa đọc
    int countByRecipientAndReadAtIsNull(User recipient);

    // Tìm theo người nhận và sắp xếp theo thời gian gửi của Notification giảm dần
    Page<NotificationRecipient> findByRecipientOrderByNotification_SentAtDesc(User recipient, Pageable pageable);

    // Tìm theo người nhận và chưa đọc, sắp xếp theo thời gian gửi của Notification giảm dần
    Page<NotificationRecipient> findByRecipientAndReadAtIsNullOrderByNotification_SentAtDesc(User recipient, Pageable pageable);

    // Tìm theo thông báo ID và người nhận
    Optional<NotificationRecipient> findByNotification_IdAndRecipient(Long notificationId, User recipient);

    // Đánh dấu tất cả thông báo là đã đọc cho một người dùng
    @Modifying
    @Query("UPDATE NotificationRecipient nr SET nr.readAt = :now WHERE nr.recipient.id = :recipientId AND nr.readAt IS NULL")
    int markAllAsRead(@Param("recipientId") Long recipientId, @Param("now") LocalDateTime now);
}