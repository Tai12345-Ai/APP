package com.fx.login.service;

import com.fx.login.model.FeeEntity;
import com.fx.login.repo.FeeRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FeeService {

    private final FeeRepo feeRepo;

    @Autowired
    public FeeService(FeeRepo feeRepo) {
        this.feeRepo = feeRepo;
    }

    // ====================================================
    // Basic CRUD Operations
    // ====================================================

    public List<FeeEntity> findAll() {
        List<FeeEntity> fees = feeRepo.findAll();
        // Sync properties cho JavaFX binding
        fees.forEach(FeeEntity::syncToProperties);
        return fees;
    }

    public Optional<FeeEntity> findById(Long id) {
        Optional<FeeEntity> fee = feeRepo.findById(id);
        fee.ifPresent(FeeEntity::syncToProperties);
        return fee;
    }

    public FeeEntity createFee(FeeEntity feeEntity) {
        // Sync từ properties trước khi save
        feeEntity.syncFromProperties();

        // Set timestamps nếu chưa có
        if (feeEntity.getCreatedDate() == null) {
            feeEntity.setCreatedDate(LocalDateTime.now());
        }
        feeEntity.setUpdatedDate(LocalDateTime.now());

        // Set default active status
        if (feeEntity.getIsActive() == null) {
            feeEntity.setIsActive(true);
        }

        FeeEntity saved = feeRepo.save(feeEntity);
        saved.syncToProperties();
        return saved;
    }

    public FeeEntity updateFee(Long id, FeeEntity feeEntity) {
        return feeRepo.findById(id)
                .map(existingFee -> {
                    // Sync từ properties trước khi update
                    feeEntity.syncFromProperties();

                    // Update fields
                    existingFee.setFeeName(feeEntity.getFeeName());
                    existingFee.setAmountDue(feeEntity.getAmountDue());
                    existingFee.setMonthlyFee(feeEntity.getMonthlyFee());
                    existingFee.setIsActive(feeEntity.getIsActive());
                    existingFee.setUpdatedDate(LocalDateTime.now());

                    FeeEntity updated = feeRepo.save(existingFee);
                    updated.syncToProperties();
                    return updated;
                })
                .orElseThrow(() -> new RuntimeException("Fee not found with id: " + id));
    }

    public void deleteFee(Long id) {
        if (!feeRepo.existsById(id)) {
            throw new RuntimeException("Fee not found with id: " + id);
        }
        feeRepo.deleteById(id);
    }

    // ====================================================
    // Custom Query Methods (cập nhật theo schema mới)
    // ====================================================

    public List<FeeEntity> findByFeeName(String feeName) {
        List<FeeEntity> fees = feeRepo.findByFeeNameContaining(feeName);
        fees.forEach(FeeEntity::syncToProperties);
        return fees;
    }

    public List<FeeEntity> findByFeeNameContaining(String feeName) {
        List<FeeEntity> fees = feeRepo.findByFeeNameContaining(feeName);
        fees.forEach(FeeEntity::syncToProperties);
        return fees;
    }

    // ====================================================
    // THÊM METHOD NÀY - searchByFeeName
    // ====================================================
    public List<FeeEntity> searchByFeeName(String feeName) {
        List<FeeEntity> fees;
        if (feeName == null || feeName.trim().isEmpty()) {
            fees = feeRepo.findAll();
        } else {
            fees = feeRepo.findByFeeNameContaining(feeName.trim());
        }
        fees.forEach(FeeEntity::syncToProperties);
        return fees;
    }

    public List<FeeEntity> findActiveFeesForResident() {
        List<FeeEntity> fees = feeRepo.findByIsActiveTrue();
        fees.forEach(FeeEntity::syncToProperties);
        return fees;
    }

    public List<FeeEntity> findAllForSelection() {
        List<FeeEntity> fees = feeRepo.findByIsActiveTrueOrderByFeeNameAsc();
        fees.forEach(FeeEntity::syncToProperties);
        return fees;
    }

    public List<FeeEntity> findByIsActive(Boolean isActive) {
        List<FeeEntity> fees = feeRepo.findByIsActive(isActive);
        fees.forEach(FeeEntity::syncToProperties);
        return fees;
    }

    public List<FeeEntity> findActiveFeesOnly() {
        return findByIsActive(true);
    }

    public List<FeeEntity> findInactiveFeesOnly() {
        return findByIsActive(false);
    }

    // ====================================================
    // Business Logic Methods
    // ====================================================

    public FeeEntity save(FeeEntity feeEntity) {
        // Sync từ properties trước khi save
        feeEntity.syncFromProperties();

        if (feeEntity.getId() == null) {
            // Tạo mới
            return createFee(feeEntity);
        } else {
            // Cập nhật
            return updateFee(feeEntity.getId(), feeEntity);
        }
    }

    public boolean existsById(Long id) {
        return feeRepo.existsById(id);
    }

    public Optional<FeeEntity> findByIdForResident(Long id) {
        // Chỉ trả về fee active cho resident
        Optional<FeeEntity> fee = feeRepo.findByIdAndIsActiveTrue(id);
        fee.ifPresent(FeeEntity::syncToProperties);
        return fee;
    }

    // ====================================================
    // Utility Methods
    // ====================================================

    public long countAll() {
        return feeRepo.count();
    }

    public long countActiveFees() {
        return feeRepo.countByIsActiveTrue();
    }

    public long countInactiveFees() {
        return feeRepo.countByIsActiveFalse();
    }

    public void activateFee(Long id) {
        feeRepo.findById(id).ifPresent(fee -> {
            fee.setIsActive(true);
            fee.setUpdatedDate(LocalDateTime.now());
            feeRepo.save(fee);
        });
    }

    public void deactivateFee(Long id) {
        feeRepo.findById(id).ifPresent(fee -> {
            fee.setIsActive(false);
            fee.setUpdatedDate(LocalDateTime.now());
            feeRepo.save(fee);
        });
    }

    // ====================================================
    // Validation Methods
    // ====================================================

    public boolean isFeeNameExists(String feeName) {
        return feeRepo.existsByFeeName(feeName);
    }

    public boolean isFeeNameExistsExcludingId(String feeName, Long excludeId) {
        return feeRepo.existsByFeeNameAndIdNot(feeName, excludeId);
    }

    public void validateFeeEntity(FeeEntity feeEntity) {
        if (feeEntity.getFeeName() == null || feeEntity.getFeeName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên khoản phí không được để trống");
        }

        if (feeEntity.getAmountDue() == null || feeEntity.getAmountDue().trim().isEmpty()) {
            throw new IllegalArgumentException("Số tiền phải đóng không được để trống");
        }

        // Kiểm tra duplicate name
        if (feeEntity.getId() == null) {
            // Tạo mới
            if (isFeeNameExists(feeEntity.getFeeName())) {
                throw new IllegalArgumentException("Tên khoản phí đã tồn tại");
            }
        } else {
            // Cập nhật
            if (isFeeNameExistsExcludingId(feeEntity.getFeeName(), feeEntity.getId())) {
                throw new IllegalArgumentException("Tên khoản phí đã tồn tại");
            }
        }
    }
}