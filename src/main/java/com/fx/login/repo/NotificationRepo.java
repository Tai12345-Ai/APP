package com.fx.login.repo;

import com.fx.login.model.Notification;
import com.fx.login.model.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface NotificationRepo extends JpaRepository<Notification, Long> {
    @EntityGraph(attributePaths = {"senderAdmin", "recipients", "recipients.recipient"})
    @Query("SELECT DISTINCT n FROM Notification n LEFT JOIN n.recipients r " +
            "WHERE (:senderId IS NULL OR n.senderAdmin.id = :senderId) " +
            "AND (:recipientFilter IS NULL OR n.recipientTargetDescription LIKE %:recipientFilter% " +
            "OR EXISTS (SELECT 1 FROM User u WHERE u.id = r.recipient.id AND " +
            "(CAST(u.id AS string) LIKE %:recipientFilter% OR u.fullname LIKE %:recipientFilter% OR u.email LIKE %:recipientFilter%))) " +
            "AND (:typeFilter IS NULL OR n.notificationType = :typeFilter) " +
            "AND (:subjectFilter IS NULL OR n.emailSubject LIKE %:subjectFilter%) " +
            "AND (:fromDate IS NULL OR n.sentAt >= :fromDate) " +
            "AND (:toDate IS NULL OR n.sentAt <= :toDate) " +
            "AND (:sentViaEmail IS NULL OR r.sentViaEmail = :sentViaEmail) " +
            "AND (:sentViaApp IS NULL OR r.sentViaApp = :sentViaApp)")
    Page<Notification> findAllWithFilters(
            @Param("senderId") Long senderId,
            @Param("recipientFilter") String recipientFilter,
            @Param("typeFilter") NotificationType typeFilter,
            @Param("subjectFilter") String subjectFilter,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("sentViaEmail") Boolean sentViaEmail,
            @Param("sentViaApp") Boolean sentViaApp,
            Pageable pageable
    );
}