package com.fx.login.model;

import javax.persistence.*;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "resident")
public class Resident {

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getApartmentNumber() {
        return apartmentNumber;
    }

    public void setApartmentNumber(String apartmentNumber) {
        this.apartmentNumber = apartmentNumber;
    }

    public long getIdProperty() {
        return idProperty.get();
    }

    public LongProperty idPropertyProperty() {
        return idProperty;
    }

    public void setIdProperty(long idProperty) {
        this.idProperty.set(idProperty);
    }

    public String getFullNameProperty() {
        return fullNameProperty.get();
    }

    public StringProperty fullNamePropertyProperty() {
        return fullNameProperty;
    }

    public void setFullNameProperty(String fullNameProperty) {
        this.fullNameProperty.set(fullNameProperty);
    }

    public String getEmailProperty() {
        return emailProperty.get();
    }

    public StringProperty emailPropertyProperty() {
        return emailProperty;
    }

    public void setEmailProperty(String emailProperty) {
        this.emailProperty.set(emailProperty);
    }

    public String getPhoneProperty() {
        return phoneProperty.get();
    }

    public StringProperty phonePropertyProperty() {
        return phoneProperty;
    }

    public void setPhoneProperty(String phoneProperty) {
        this.phoneProperty.set(phoneProperty);
    }

    public String getApartmentNumberProperty() {
        return apartmentNumberProperty.get();
    }

    public StringProperty apartmentNumberPropertyProperty() {
        return apartmentNumberProperty;
    }

    public void setApartmentNumberProperty(String apartmentNumberProperty) {
        this.apartmentNumberProperty.set(apartmentNumberProperty);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "apartment_number")
    private String apartmentNumber;

    // JavaFX properties
    @Transient
    private LongProperty idProperty;
    @Transient
    private StringProperty fullNameProperty;
    @Transient
    private StringProperty emailProperty;
    @Transient
    private StringProperty phoneProperty;
    @Transient
    private StringProperty apartmentNumberProperty;

    // Constructor mặc định
    public Resident() {
        this.idProperty = new SimpleLongProperty();
        this.fullNameProperty = new SimpleStringProperty();
        this.emailProperty = new SimpleStringProperty();
        this.phoneProperty = new SimpleStringProperty();
        this.apartmentNumberProperty = new SimpleStringProperty();

        // Đồng bộ 2 chiều
        this.fullNameProperty.addListener((obs, oldVal, newVal) -> this.fullName = newVal);
        this.emailProperty.addListener((obs, oldVal, newVal) -> this.email = newVal);
        this.phoneProperty.addListener((obs, oldVal, newVal) -> this.phone = newVal);
        this.apartmentNumberProperty.addListener((obs, oldVal, newVal) -> this.apartmentNumber = newVal);
        this.idProperty.addListener((obs, oldVal, newVal) -> this.id = newVal.longValue());
    }


    public Resident(Long id, String fullName, String email, String phone, String apartmentNumber) {
        this();
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.apartmentNumber = apartmentNumber;
        syncProperties();  // Đồng bộ ngay lập tức khi khởi tạo
    }

    // Getter JavaFX Property
    public LongProperty idProperty() {
        if (idProperty == null) idProperty = new SimpleLongProperty(id != null ? id : 0);
        return idProperty;
    }

    public StringProperty fullNameProperty() {
        if (fullNameProperty == null) fullNameProperty = new SimpleStringProperty(fullName);
        return fullNameProperty;
    }

    public StringProperty emailProperty() {
        if (emailProperty == null) emailProperty = new SimpleStringProperty(email);
        return emailProperty;
    }

    public StringProperty phoneProperty() {
        if (phoneProperty == null) phoneProperty = new SimpleStringProperty(phone);
        return phoneProperty;
    }

    public StringProperty apartmentNumberProperty() {
        if (apartmentNumberProperty == null) apartmentNumberProperty = new SimpleStringProperty(apartmentNumber);
        return apartmentNumberProperty;
    }

    // Đồng bộ dữ liệu JavaFX Property với các field thông thường
    public void syncProperties() {
        idProperty().set(id != null ? id : 0);
        fullNameProperty().set(fullName != null ? fullName : "");
        emailProperty().set(email != null ? email : "");
        phoneProperty().set(phone != null ? phone : "");
        apartmentNumberProperty().set(apartmentNumber != null ? apartmentNumber : "");
    }
}
