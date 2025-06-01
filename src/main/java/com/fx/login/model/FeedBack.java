package com.fx.login.model;

import javax.persistence.*;
import javafx.beans.property.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Getter
@Setter
@Table(name = "feedback")
public class FeedBack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    private String title;

    private String content;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private String status;

    private String sender;

    private boolean visibleToUser = true;  // Mặc định là hiển thị

    public boolean isVisibleToUser() {
        return visibleToUser;
    }

    // JavaFX properties
    @Transient
    private LongProperty idProperty;
    @Transient
    private StringProperty titleProperty;
    @Transient
    private StringProperty contentProperty;
    @Transient
    private StringProperty createdAtProperty;
    @Transient
    private StringProperty statusProperty;
    @Transient
    private StringProperty senderProperty;
    @Transient
    private BooleanProperty visibleToUserProperty;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    // Constructor mặc định
    public FeedBack() {
        this.idProperty = new SimpleLongProperty();
        this.titleProperty = new SimpleStringProperty();
        this.contentProperty = new SimpleStringProperty();
        this.createdAtProperty = new SimpleStringProperty();
        this.statusProperty = new SimpleStringProperty();
        this.senderProperty = new SimpleStringProperty();
        this.visibleToUserProperty = new SimpleBooleanProperty(true);

        // Đồng bộ 2 chiều
        this.titleProperty.addListener((obs, oldVal, newVal) -> this.title = newVal);
        this.contentProperty.addListener((obs, oldVal_, newVal) -> this.content = newVal);
        this.createdAtProperty.addListener((obs, oldVal, newVal) -> {
            try {
                this.createdAt = LocalDateTime.parse(newVal, formatter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        this.statusProperty.addListener((obs, oldVal, newVal) -> this.status = newVal);
        this.senderProperty.addListener((obs, oldVal, newVal) -> this.sender = newVal);
        this.idProperty.addListener((obs, oldVal, newVal) -> this.id = newVal.longValue());
        this.visibleToUserProperty.addListener((obs, oldVal, newVal) -> this.visibleToUser = newVal);
    }

    public FeedBack(Long id, String title, String content, LocalDateTime createdAt, String status, String sender, boolean visibleToUser) {
        this();
        this.id = id;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.status = status;
        this.sender = sender;
        this.visibleToUser = visibleToUser;
        syncProperties();  // Đồng bộ khi khởi tạo
    }

    // Getter JavaFX Property
    public LongProperty idProperty() {
        if (idProperty == null) idProperty = new SimpleLongProperty(id != null ? id : 0);
        return idProperty;
    }

    public StringProperty titleProperty() {
        if (titleProperty == null) titleProperty = new SimpleStringProperty(title != null ? title : "");
        return titleProperty;
    }

    public StringProperty contentProperty() {
        if (contentProperty == null) contentProperty = new SimpleStringProperty(content != null ? content : "");
        return contentProperty;
    }

    public StringProperty createdAtProperty() {
        if (createdAtProperty == null) {
            createdAtProperty = new SimpleStringProperty(
                    createdAt != null ? formatter.format(createdAt) : ""
            );
        }
        return createdAtProperty;
    }

    public StringProperty statusProperty() {
        if (statusProperty == null) statusProperty = new SimpleStringProperty(status != null ? status : "");
        return statusProperty;
    }

    public StringProperty senderProperty() {
        if (senderProperty == null) senderProperty = new SimpleStringProperty(sender != null ? sender : "");
        return senderProperty;
    }

    public BooleanProperty visibleToUserProperty() {
        if (visibleToUserProperty == null) visibleToUserProperty = new SimpleBooleanProperty(visibleToUser);
        return visibleToUserProperty;
    }

    // Đồng bộ dữ liệu từ entity sang property
    public void syncProperties() {
        idProperty().set(id != null ? id : 0);
        titleProperty().set(title != null ? title : "");
        contentProperty().set(content != null ? content : "");
        createdAtProperty().set(createdAt != null ? formatter.format(createdAt) : "");
        statusProperty().set(status != null ? status : "");
        senderProperty().set(sender != null ? sender : "");
        visibleToUserProperty().set(visibleToUser);
    }
}