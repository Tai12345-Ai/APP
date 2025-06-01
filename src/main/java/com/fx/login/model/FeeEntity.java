package com.fx.login.model;

import javafx.beans.property.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "fee")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "fee_name", nullable = false)
    private String feeName;
    
    @Column(name = "amount_due", nullable = false)
    private String amountDue;
    
    @Column(name = "monthly_fee")
    private String monthlyFee;
    
    @Column(name = "created_date")
    private LocalDateTime createdDate;
    
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    // ====================================================
    // JavaFX Properties cho UI binding
    // ====================================================
    
    @Transient
    private LongProperty idProperty;
    
    @Transient
    private StringProperty feeNameProperty;
    
    @Transient
    private StringProperty amountDueProperty;
    
    @Transient
    private StringProperty monthlyFeeProperty;
    
    @Transient
    private BooleanProperty isActiveProperty;
    
    // ====================================================
    // Business Logic Properties (if needed)
    // ====================================================
    
    @Transient
    private List<Object> unpaidHouseholds; // Temporary placeholder
    
    // ====================================================
    // Getters cho JavaFX Properties
    // ====================================================
    
    public LongProperty idProperty() {
        if (idProperty == null) {
            idProperty = new SimpleLongProperty(this, "id", id != null ? id : 0L);
        }
        return idProperty;
    }
    
    public StringProperty feeNameProperty() {
        if (feeNameProperty == null) {
            feeNameProperty = new SimpleStringProperty(this, "feeName", feeName);
        }
        return feeNameProperty;
    }
    
    public StringProperty amountDueProperty() {
        if (amountDueProperty == null) {
            amountDueProperty = new SimpleStringProperty(this, "amountDue", amountDue);
        }
        return amountDueProperty;
    }
    
    public StringProperty monthlyFeeProperty() {
        if (monthlyFeeProperty == null) {
            monthlyFeeProperty = new SimpleStringProperty(this, "monthlyFee", monthlyFee);
        }
        return monthlyFeeProperty;
    }
    
    public BooleanProperty isActiveProperty() {
        if (isActiveProperty == null) {
            isActiveProperty = new SimpleBooleanProperty(this, "isActive", isActive != null ? isActive : true);
        }
        return isActiveProperty;
    }
    
    // ====================================================
    // Business Logic Methods
    // ====================================================
    
    /**
     * Lấy danh sách hộ gia đình chưa đóng phí này
     * TODO: Implement proper logic with HouseholdEntity and PaymentEntity
     */
    public List<Object> getUnpaidHouseholds() {
        if (unpaidHouseholds == null) {
            unpaidHouseholds = new ArrayList<>();
        }
        return unpaidHouseholds;
    }
    
    /**
     * Set danh sách hộ gia đình chưa đóng phí
     */
    public void setUnpaidHouseholds(List<Object> unpaidHouseholds) {
        this.unpaidHouseholds = unpaidHouseholds;
    }
    
    /**
     * THÊM METHOD NÀY - để set unpaidHouseholds từ String count
     */
    public void setUnpaidHouseholdsCount(String count) {
        // Convert string count thành một list có size tương ứng
        if (unpaidHouseholds == null) {
            unpaidHouseholds = new ArrayList<>();
        }
        try {
            int size = Integer.parseInt(count);
            unpaidHouseholds.clear();
            for (int i = 0; i < size; i++) {
                unpaidHouseholds.add(new Object()); // Placeholder objects
            }
        } catch (NumberFormatException e) {
            unpaidHouseholds.clear();
        }
    }
    
    /**
     * Đếm số hộ gia đình chưa đóng phí
     */
    public int getUnpaidHouseholdCount() {
        return getUnpaidHouseholds().size();
    }
    
    /**
     * Kiểm tra có hộ gia đình nào chưa đóng phí không
     */
    public boolean hasUnpaidHouseholds() {
        return getUnpaidHouseholdCount() > 0;
    }
    
    /**
     * Tính tổng số tiền chưa thu được
     */
    public double getTotalUnpaidAmount() {
        try {
            double amount = Double.parseDouble(amountDue.replace(",", ""));
            return amount * getUnpaidHouseholdCount();
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    /**
     * Format số tiền hiển thị
     */
    public String getFormattedAmountDue() {
        try {
            double amount = Double.parseDouble(amountDue.replace(",", ""));
            return String.format("%,.0f", amount);
        } catch (NumberFormatException e) {
            return amountDue;
        }
    }
    
    /**
     * Format phí hàng tháng hiển thị
     */
    public String getFormattedMonthlyFee() {
        if (monthlyFee == null || monthlyFee.trim().isEmpty()) {
            return "N/A";
        }
        try {
            double amount = Double.parseDouble(monthlyFee.replace(",", ""));
            return String.format("%,.0f", amount);
        } catch (NumberFormatException e) {
            return monthlyFee;
        }
    }
    
    // ====================================================
    // Sync methods để đồng bộ giữa fields và properties
    // ====================================================
    
    public void syncFromProperties() {
        if (idProperty != null) {
            this.id = idProperty.get();
        }
        if (feeNameProperty != null) {
            this.feeName = feeNameProperty.get();
        }
        if (amountDueProperty != null) {
            this.amountDue = amountDueProperty.get();
        }
        if (monthlyFeeProperty != null) {
            this.monthlyFee = monthlyFeeProperty.get();
        }
        if (isActiveProperty != null) {
            this.isActive = isActiveProperty.get();
        }
    }
    
    public void syncToProperties() {
        idProperty().set(id != null ? id : 0L);
        feeNameProperty().set(feeName);
        amountDueProperty().set(amountDue);
        monthlyFeeProperty().set(monthlyFee);
        isActiveProperty().set(isActive != null ? isActive : true);
    }
    
    /**
     * THÊM METHOD NÀY - alias cho syncToProperties để tương thích
     */
    public void syncProperties() {
        syncToProperties();
    }
    
    // ====================================================
    // JPA Lifecycle callbacks
    // ====================================================
    
    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        updatedDate = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
        syncFromProperties(); // Đồng bộ từ properties trước khi update
    }
    
    @PostLoad
    protected void onLoad() {
        syncToProperties(); // Đồng bộ sang properties sau khi load từ DB
    }
    
    // ====================================================
    // Override toString for debugging
    // ====================================================
    
    @Override
    public String toString() {
        return "FeeEntity{" +
                "id=" + id +
                ", feeName='" + feeName + '\'' +
                ", amountDue='" + amountDue + '\'' +
                ", monthlyFee='" + monthlyFee + '\'' +
                ", isActive=" + isActive +
                '}';
    }
    
    // ====================================================
    // Equals and HashCode based on business key
    // ====================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FeeEntity)) return false;
        
        FeeEntity feeEntity = (FeeEntity) o;
        
        if (id != null) {
            return id.equals(feeEntity.id);
        }
        
        return feeName != null ? feeName.equals(feeEntity.feeName) : feeEntity.feeName == null;
    }
    
    @Override
    public int hashCode() {
        if (id != null) {
            return id.hashCode();
        }
        return feeName != null ? feeName.hashCode() : 0;
    }
}