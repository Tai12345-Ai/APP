package com.fx.login.service;

import com.fx.login.dto.NotificationHistoryViewDTO;
import com.fx.login.dto.ResidentNotificationViewDTO;
import com.fx.login.model.Notification;
import com.fx.login.model.NotificationRecipient;
import com.fx.login.model.NotificationType;
import com.fx.login.model.User;
import com.fx.login.repo.NotificationRecipientRepo;
import com.fx.login.repo.NotificationRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepo notificationRepository;

    @Autowired
    private NotificationRecipientRepo notificationRecipientRepository;

    @Autowired
    private EmailService emailService;

    @Override
    @Transactional
    public Notification createAndSendNotification(User senderAdmin, List<User> recipients,
                                                  String recipientTargetDescription, NotificationType notificationType,
                                                  String subject, String content, String linkUrl,
                                                  boolean sendViaEmail, boolean sendViaInApp) {

        // 1. Tạo thông báo chính
        Notification notification = new Notification();
        notification.setSenderAdmin(senderAdmin);
        notification.setNotificationType(notificationType);
        notification.setEmailSubject(subject);
        notification.setMessageContent(content);
        notification.setLinkUrl(linkUrl);
        notification.setRecipientTargetDescription(recipientTargetDescription);

        // 2. Lưu notification trước để có id
        notification = notificationRepository.save(notification);

        // 3. Tạo các bản ghi recipient
        List<NotificationRecipient> notificationRecipients = new ArrayList<>();

        for (User recipient : recipients) {
            NotificationRecipient recipientEntry = new NotificationRecipient();
            recipientEntry.setNotification(notification);
            recipientEntry.setRecipient(recipient);
            recipientEntry.setSentViaEmail(sendViaEmail);
            recipientEntry.setSentViaApp(sendViaInApp);

            notificationRecipients.add(recipientEntry);

            // 4. Gửi email nếu được yêu cầu
            if (sendViaEmail) {
                try {
                    emailService.sendNotificationEmail(recipient.getEmail(), subject, content,linkUrl, notificationType.getDisplayName()
                    );
                } catch (Exception e) {
                    // Log lỗi nhưng vẫn tiếp tục xử lý các người nhận khác
                    System.err.println("Không thể gửi email đến " + recipient.getEmail() + ": " + e.getMessage());
                }
            }
        }

        // 5. Lưu tất cả recipient
        notificationRecipientRepository.saveAll(notificationRecipients);

        // Cập nhật danh sách recipients trong đối tượng notification
        notification.setRecipients(notificationRecipients);

        return notification;
    }

    @Override
    public Page<NotificationHistoryViewDTO> getNotificationHistoryForAdmin(
            String senderFilter, String recipientFilter, NotificationType typeFilter,
            String subjectFilter, LocalDate dateFrom, LocalDate dateTo,
            Boolean sentViaEmail, Boolean sentViaApp, Pageable pageable) {

        // Xử lý các điều kiện tìm kiếm
        Long senderId = null;
        if (senderFilter != null && !senderFilter.trim().isEmpty()) {
            try {
                senderId = Long.parseLong(senderFilter.trim());
            } catch (NumberFormatException e) {
                // Không phải số, bỏ qua
            }
        }

        // Chuyển đổi LocalDate thành LocalDateTime cho truy vấn
        LocalDateTime fromDateTime = null;
        if (dateFrom != null) {
            fromDateTime = LocalDateTime.of(dateFrom, LocalTime.MIN);
        }

        LocalDateTime toDateTime = null;
        if (dateTo != null) {
            toDateTime = LocalDateTime.of(dateTo, LocalTime.MAX);
        }

        // Lấy dữ liệu từ repository
        Page<Notification> notificationsPage = notificationRepository.findAllWithFilters(
                senderId, recipientFilter, typeFilter, subjectFilter,
                fromDateTime, toDateTime, sentViaEmail, sentViaApp, pageable);

        // Chuyển đổi thành DTO
        List<NotificationHistoryViewDTO> dtoList = notificationsPage.getContent().stream()
                .map(this::convertToHistoryDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, notificationsPage.getTotalElements());
    }

    private NotificationHistoryViewDTO convertToHistoryDTO(Notification notification) {
        NotificationHistoryViewDTO dto = new NotificationHistoryViewDTO();
        dto.setId(notification.getId());
        dto.setSentAt(notification.getSentAt());
        if (notification.getSenderAdmin() != null) {
            dto.setSenderAdminInfo(notification.getSenderAdmin().getId() + " - " + notification.getSenderAdmin().getFullname());
        } else {
            dto.setSenderAdminInfo("Hệ thống");
        }
        dto.setRecipientTargetDescription(notification.getRecipientTargetDescription());
        dto.setNotificationType(notification.getNotificationType().getDisplayName());
        dto.setEmailSubject(notification.getEmailSubject());
        String content = notification.getMessageContent();
        if (content != null && content.length() > 100) content = content.substring(0, 97) + "...";
        dto.setContentSnippet(content);
        dto.setLinkUrl(notification.getLinkUrl());

        boolean sentViaEmail = false, sentViaApp = false;
        if (notification.getRecipients() != null && !notification.getRecipients().isEmpty()) {
            sentViaEmail = notification.getRecipients().stream().anyMatch(NotificationRecipient::isSentViaEmail);
            sentViaApp = notification.getRecipients().stream().anyMatch(NotificationRecipient::isSentViaApp);
        }
        dto.setSentViaEmail(sentViaEmail);
        dto.setSentViaApp(sentViaApp);

        dto.initProperties();
        return dto;
    }

    @Override
    public Notification getNotificationById(Long id) {
        return notificationRepository.findById(id).orElse(null);
    }

    @Override
    public Page<NotificationRecipient> getNotificationsForUser(User user, boolean unreadOnly, Pageable pageable) {
        if (unreadOnly) {
            return notificationRecipientRepository.findByRecipientAndReadAtIsNullOrderBySentAtDesc(user, pageable);
        } else {
            return notificationRecipientRepository.findByRecipientOrderBySentAtDesc(user, pageable);
        }
    }

    @Override
    @Transactional
    public NotificationRecipient markAsRead(Long notificationId, User user) {
        Optional<NotificationRecipient> recipientOpt = notificationRecipientRepository
                .findByNotification_IdAndRecipient(notificationId, user);

        if (recipientOpt.isPresent()) {
            NotificationRecipient recipient = recipientOpt.get();
            if (recipient.getReadAt() == null) {
                recipient.setReadAt(LocalDateTime.now());
                return notificationRecipientRepository.save(recipient);
            }
            return recipient;
        }
        return null;
    }

    @Override
    @Transactional
    public int markAllAsRead(User user) {
        List<NotificationRecipient> unreadNotifications = notificationRecipientRepository
                .findByRecipientAndReadAtIsNull(user);

        LocalDateTime now = LocalDateTime.now();
        int count = 0;

        for (NotificationRecipient recipient : unreadNotifications) {
            recipient.setReadAt(now);
            notificationRecipientRepository.save(recipient);
            count++;
        }

        return count;
    }

    @Override
    public int countUnreadNotifications(User user) {
        return notificationRecipientRepository.countByRecipientAndReadAtIsNull(user);
    }
    @Override
    public Page<ResidentNotificationViewDTO> getNotificationsForResident(
            User resident, boolean unreadOnly, Pageable pageable) {

        Page<NotificationRecipient> recipients;
        if (unreadOnly) {
            recipients = notificationRecipientRepository.findByRecipientAndReadAtIsNullFetchNotification(resident, pageable);
        } else {
            recipients = notificationRecipientRepository.findByRecipientFetchNotification(resident, pageable);
        }

        List<ResidentNotificationViewDTO> dtoList = recipients.getContent().stream()
                .map(this::convertToResidentDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, recipients.getTotalElements());
    }

    // convertToResidentDTO (giữ nguyên, KHÔNG bị lazy nữa)
    private ResidentNotificationViewDTO convertToResidentDTO(NotificationRecipient recipient) {
        ResidentNotificationViewDTO dto = new ResidentNotificationViewDTO();
        Notification notification = recipient.getNotification();

        dto.setId(recipient.getId());
        dto.setNotificationId(notification.getId());
        dto.setNotificationType(notification.getNotificationType().getDisplayName());
        dto.setSubject(notification.getEmailSubject());
        dto.setContent(notification.getMessageContent());
        dto.setLink(notification.getLinkUrl());
        dto.setSentAt(notification.getSentAt());

        if (notification.getSenderAdmin() != null) {
            dto.setSenderName(notification.getSenderAdmin().getFullname());
        } else {
            dto.setSenderName("Hệ thống");
        }

        dto.setRead(recipient.getReadAt() != null);
        dto.setReadAt(recipient.getReadAt());

        // Update JavaFX properties
        dto.updateProperties();

        return dto;
    }

    @Override
    @Transactional
    public boolean markAsRead(User resident, Long notificationRecipientId) {
        Optional<NotificationRecipient> recipientOpt = notificationRecipientRepository.findById(notificationRecipientId);

        if (recipientOpt.isPresent()) {
            NotificationRecipient recipient = recipientOpt.get();

            // Kiểm tra xem người dùng có phải là người nhận của thông báo này không
            if (recipient.getRecipient().getId() == resident.getId()) {
                if (recipient.getReadAt() == null) {
                    recipient.setReadAt(LocalDateTime.now());
                    notificationRecipientRepository.save(recipient);
                }
                return true;
            }
        }

        return false;
    }

    @Override
    @Transactional
    public int markAllAsReadForResident(User resident) {
        return notificationRecipientRepository.markAllAsRead(resident.getId(), LocalDateTime.now());
    }

    @Override
    public long countUnreadNotificationsForResident(User resident) {
        return notificationRecipientRepository.countByRecipientAndReadAtIsNull(resident);
    }
}