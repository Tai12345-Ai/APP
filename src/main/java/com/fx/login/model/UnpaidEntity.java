
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
@Table(name = "unpaid")
public class UnpaidEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    private String residentName;

    private String apartmentName;

    private String totalPayment;

    private String dueDate;

    private Long feeID;

    @Column(name = "status")
    private String status; // "PENDING", "COMPLETED", "CANCELED"

    @Column(name = "description")
    private String description;

    // JavaFX properties
    @Transient
    private LongProperty idProperty;
    @Transient
    private StringProperty residentNameProperty;
    @Transient
    private StringProperty apartmentNameProperty;
    @Transient
    private StringProperty totalPaymentProperty;
    @Transient
    private StringProperty dueDateProperty;
    @Transient
    private LongProperty feeIDProperty;
    @Transient
    private StringProperty statusProperty;
    @Transient
    private StringProperty descriptionProperty;

    // Constructor mặc định
    public UnpaidEntity() {
        this.idProperty = new SimpleLongProperty();
        this.residentNameProperty = new SimpleStringProperty();
        this.apartmentNameProperty = new SimpleStringProperty();
        this.totalPaymentProperty = new SimpleStringProperty();
        this.dueDateProperty = new SimpleStringProperty();
        this.feeIDProperty = new SimpleLongProperty();
        this.statusProperty = new SimpleStringProperty();
        this.descriptionProperty = new SimpleStringProperty();

        // Đồng bộ 2 chiều
        this.residentNameProperty.addListener((obs, oldVal, newVal) -> this.residentName = newVal);
        this.apartmentNameProperty.addListener((obs, oldVal, newVal) -> this.apartmentName = newVal);
        this.totalPaymentProperty.addListener((obs, oldVal, newVal) -> this.totalPayment = newVal);
        this.dueDateProperty.addListener((obs, oldVal, newVal) -> this.dueDate = newVal);
        this.idProperty.addListener((obs, oldVal, newVal) -> this.id = newVal.longValue());
        this.feeIDProperty.addListener((obs, oldVal, newVal) -> this.feeID = newVal.longValue());
        this.statusProperty.addListener((obs, oldVal, newVal) -> this.status = newVal);
        this.descriptionProperty.addListener((obs, oldVal, newVal) -> this.description = newVal);
    }


    public UnpaidEntity(Long id, String residentName, String apartmentName, String totalPayment, String dueDate) {
        this();
        this.id = id;
        this.residentName = residentName;
        this.apartmentName = apartmentName;
        this.totalPayment = totalPayment;
        this.dueDate = dueDate;
        this.status = "PENDING"; // Default status
        syncProperties();  // Đồng bộ ngay lập tức khi khởi tạo
    }

    public UnpaidEntity(Long id, String residentName, String apartmentName, String totalPayment, String dueDate, String status, String description) {
        this();
        this.id = id;
        this.residentName = residentName;
        this.apartmentName = apartmentName;
        this.totalPayment = totalPayment;
        this.dueDate = dueDate;
        this.status = status;
        this.description = description;
        syncProperties();  // Đồng bộ ngay lập tức khi khởi tạo
    }

    // Getter và setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getResidentName() {
        return residentName;
    }

    public void setResidentName(String residentName) {
        this.residentName = residentName;
    }

    public String getApartmentName() {
        return apartmentName;
    }

    public void setApartmentName(String apartmentName) {
        this.apartmentName = apartmentName;
    }

    public String getTotalPayment() {
        return totalPayment;
    }

    public void setTotalPayment(String totalPayment) {
        this.totalPayment = totalPayment;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public Long getFeeID() {
        return feeID;
    }

    public void setFeeID(Long feeID) {
        this.feeID = feeID;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Getter/setter for JavaFX properties
    public long getIdProperty() {
        return idProperty.get();
    }

    public LongProperty idPropertyProperty() {
        return idProperty;
    }

    public void setIdProperty(long idProperty) {
        this.idProperty.set(idProperty);
    }

    public String getResidentNameProperty() {
        return residentNameProperty.get();
    }

    public StringProperty residentNamePropertyProperty() {
        return residentNameProperty;
    }

    public void setResidentNameProperty(String residentNameProperty) {
        this.residentNameProperty.set(residentNameProperty);
    }

    public String getApartmentNameProperty() {
        return apartmentNameProperty.get();
    }

    public StringProperty apartmentNamePropertyProperty() {
        return apartmentNameProperty;
    }

    public void setApartmentNameProperty(String apartmentNameProperty) {
        this.apartmentNameProperty.set(apartmentNameProperty);
    }

    public String getTotalPaymentProperty() {
        return totalPaymentProperty.get();
    }

    public StringProperty totalPaymentPropertyProperty() {
        return totalPaymentProperty;
    }

    public void setTotalPaymentProperty(String totalPaymentProperty) {
        this.totalPaymentProperty.set(totalPaymentProperty);
    }

    public String getDueDateProperty() {
        return dueDateProperty.get();
    }

    public StringProperty dueDatePropertyProperty() {
        return dueDateProperty;
    }

    public void setDueDateProperty(String dueDateProperty) {
        this.dueDateProperty.set(dueDateProperty);
    }

    public long getFeeIDProperty() {
        return feeIDProperty.get();
    }

    public LongProperty feeIDPropertyProperty() {
        return feeIDProperty;
    }

    public void setFeeIDProperty(long feeIDProperty) {
        this.feeIDProperty.set(feeIDProperty);
    }

    public String getStatusProperty() {
        return statusProperty.get();
    }

    public StringProperty statusPropertyProperty() {
        return statusProperty;
    }

    public void setStatusProperty(String statusProperty) {
        this.statusProperty.set(statusProperty);
    }

    public String getDescriptionProperty() {
        return descriptionProperty.get();
    }

    public StringProperty descriptionPropertyProperty() {
        return descriptionProperty;
    }

    public void setDescriptionProperty(String descriptionProperty) {
        this.descriptionProperty.set(descriptionProperty);
    }

    // Getter JavaFX Property
    public LongProperty idProperty() {
        if (idProperty == null) idProperty = new SimpleLongProperty(id != null ? id : 0);
        return idProperty;
    }

    public LongProperty feeIDProperty() {
        if (feeIDProperty == null) feeIDProperty = new SimpleLongProperty(feeID != null ? feeID : 0);
        return feeIDProperty;
    }

    public StringProperty residentNameProperty() {
        if (residentNameProperty == null) residentNameProperty = new SimpleStringProperty(residentName);
        return residentNameProperty;
    }

    public StringProperty apartmentNameProperty() {
        if (apartmentNameProperty == null) apartmentNameProperty = new SimpleStringProperty(apartmentName);
        return apartmentNameProperty;
    }

    public StringProperty totalPaymentProperty() {
        if (totalPaymentProperty == null) totalPaymentProperty = new SimpleStringProperty(totalPayment);
        return totalPaymentProperty;
    }

    public StringProperty dueDateProperty() {
        if (dueDateProperty == null) dueDateProperty = new SimpleStringProperty(dueDate);
        return dueDateProperty;
    }

    public StringProperty statusProperty() {
        if (statusProperty == null) statusProperty = new SimpleStringProperty(status);
        return statusProperty;
    }

    public StringProperty descriptionProperty() {
        if (descriptionProperty == null) descriptionProperty = new SimpleStringProperty(description);
        return descriptionProperty;
    }

    // Đồng bộ dữ liệu JavaFX Property với các field thông thường
    public void syncProperties() {
        idProperty().set(id != null ? id : 0);
        residentNameProperty().set(residentName != null ? residentName : "");
        apartmentNameProperty().set(apartmentName != null ? apartmentName : "");
        totalPaymentProperty().set(totalPayment != null ? totalPayment : "");
        dueDateProperty().set(dueDate != null ? dueDate : "");
        feeIDProperty().set(feeID != null ? feeID : 0);
        statusProperty().set(status != null ? status : "PENDING");
        descriptionProperty().set(description != null ? description : "");
    }
}