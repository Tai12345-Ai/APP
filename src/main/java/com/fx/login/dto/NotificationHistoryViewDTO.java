package com.fx.login.dto;

import javafx.beans.property.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class NotificationHistoryViewDTO {
    private Long notificationId;
    private LocalDateTime sentAt;
    private String senderAdminInfo;
    private String recipientTargetDescription;
    private String notificationType;
    private String emailSubject;
    private String contentSnippet;
    private String linkUrl;
    private boolean sentViaEmail;
    private boolean sentViaApp;

    // JavaFX properties for TableView
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty sentAtStr = new SimpleStringProperty();
    private final StringProperty senderAdmin = new SimpleStringProperty();
    private final StringProperty recipient = new SimpleStringProperty();
    private final StringProperty type = new SimpleStringProperty();
    private final StringProperty subject = new SimpleStringProperty();
    private final StringProperty contentSnip = new SimpleStringProperty();
    private final StringProperty sendChannels = new SimpleStringProperty();
    private final StringProperty link = new SimpleStringProperty();
    private final BooleanProperty email = new SimpleBooleanProperty();
    private final BooleanProperty app = new SimpleBooleanProperty();

    // Getters for JavaFX properties
    public LongProperty idProperty() { return id; }
    public StringProperty sentAtProperty() { return sentAtStr; }
    public StringProperty senderAdminProperty() { return senderAdmin; }
    public StringProperty recipientProperty() { return recipient; }
    public StringProperty typeProperty() { return type; }
    public StringProperty subjectProperty() { return subject; }
    public StringProperty contentSnippetProperty() { return contentSnip; }
    public StringProperty sendChannelsProperty() { return sendChannels; }
    public StringProperty linkProperty() { return link; }
    public BooleanProperty sentViaEmailProperty() { return email; }
    public BooleanProperty sentViaAppProperty() { return app; }

    // Setter for ID with update to property
    public void setId(Long id) {
        this.notificationId = id;
        this.id.set(id);
    }

    // Method to initialize all properties
    public void initProperties() {
        id.set(notificationId);
        sentAtStr.set(sentAt != null ? sentAt.toString() : "");
        senderAdmin.set(senderAdminInfo);
        recipient.set(recipientTargetDescription);
        type.set(notificationType);
        subject.set(emailSubject);
        contentSnip.set(contentSnippet);

        // Build channel info
        StringBuilder channels = new StringBuilder();
        if (sentViaEmail) channels.append("Email");
        if (sentViaApp) {
            if (channels.length() > 0) channels.append(", ");
            channels.append("Hệ thống");
        }
        sendChannels.set(channels.toString());

        link.set(linkUrl);
        email.set(sentViaEmail);
        app.set(sentViaApp);
    }
}