package com.fx.login.model;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javafx.beans.property.*;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "payment")
public class PaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "unpaid_id")
    private UnpaidEntity unpaid;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "status")
    private String status; // "PENDING", "COMPLETED", "FAILED", "PARTIAL"

    @Column(name = "notes")
    private String notes;

    // ✅ THÊM: Thông tin người thanh toán
    @Column(name = "paid_by")
    private String paidBy;

    // JavaFX properties
    @Transient
    private LongProperty idProperty;
    @Transient
    private StringProperty paymentDateProperty;
    @Transient
    private StringProperty amountProperty;
    @Transient
    private StringProperty statusProperty;
    @Transient
    private StringProperty paymentMethodProperty;
    @Transient
    private StringProperty notesProperty;
    @Transient
    private StringProperty paidByProperty;

    public PaymentEntity() {
        this.idProperty = new SimpleLongProperty();
        this.paymentDateProperty = new SimpleStringProperty();
        this.amountProperty = new SimpleStringProperty();
        this.statusProperty = new SimpleStringProperty();
        this.paymentMethodProperty = new SimpleStringProperty();
        this.notesProperty = new SimpleStringProperty();
        this.paidByProperty = new SimpleStringProperty();
    }

    // ✅ THÊM: Constructor với thông tin đầy đủ
    public PaymentEntity(UnpaidEntity unpaid, BigDecimal amount, String paymentMethod, String paidBy) {
        this();
        this.unpaid = unpaid;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paidBy = paidBy;
        this.paymentDate = LocalDateTime.now();
        this.status = "PENDING";
    }

    // Synchronize JPA and JavaFX properties
    public void syncProperties() {
        if (id != null) idProperty.set(id);
        if (paymentDate != null) paymentDateProperty.set(paymentDate.toString());
        if (amount != null) amountProperty.set(amount.toString());
        if (status != null) statusProperty.set(status);
        if (paymentMethod != null) paymentMethodProperty.set(paymentMethod);
        if (notes != null) notesProperty.set(notes);
        if (paidBy != null) paidByProperty.set(paidBy);
    }

    // JavaFX Property getters
    public LongProperty idProperty() {
        if (idProperty == null) idProperty = new SimpleLongProperty(id != null ? id : 0);
        return idProperty;
    }

    public StringProperty paymentDateProperty() {
        if (paymentDateProperty == null)
            paymentDateProperty = new SimpleStringProperty(paymentDate != null ? paymentDate.toString() : "");
        return paymentDateProperty;
    }

    public StringProperty amountProperty() {
        if (amountProperty == null)
            amountProperty = new SimpleStringProperty(amount != null ? amount.toString() : "");
        return amountProperty;
    }

    public StringProperty statusProperty() {
        if (statusProperty == null)
            statusProperty = new SimpleStringProperty(status != null ? status : "");
        return statusProperty;
    }

    public StringProperty paymentMethodProperty() {
        if (paymentMethodProperty == null)
            paymentMethodProperty = new SimpleStringProperty(paymentMethod != null ? paymentMethod : "");
        return paymentMethodProperty;
    }

    public StringProperty notesProperty() {
        if (notesProperty == null)
            notesProperty = new SimpleStringProperty(notes != null ? notes : "");
        return notesProperty;
    }

    public StringProperty paidByProperty() {
        if (paidByProperty == null)
            paidByProperty = new SimpleStringProperty(paidBy != null ? paidBy : "");
        return paidByProperty;
    }
}