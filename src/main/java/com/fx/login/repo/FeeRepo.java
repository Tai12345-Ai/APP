package com.fx.login.repo;

import com.fx.login.model.FeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeeRepo extends JpaRepository<FeeEntity, Long> {
    
    // ====================================================
    // Tìm theo fee name
    // ====================================================
    
    /**
     * Tìm khoản phí theo tên chứa từ khóa
     */
    List<FeeEntity> findByFeeNameContaining(String feeName);
    
    /**
     * Tìm khoản phí theo tên chính xác
     */
    List<FeeEntity> findByFeeName(String feeName);
    
    /**
     * Tìm khoản phí theo tên (ignore case)
     */
    List<FeeEntity> findByFeeNameIgnoreCase(String feeName);
    
    /**
     * Tìm khoản phí theo tên chứa từ khóa (ignore case)
     */
    List<FeeEntity> findByFeeNameContainingIgnoreCase(String feeName);
    
    // ====================================================
    // Tìm theo active status
    // ====================================================
    
    /**
     * Tìm theo trạng thái active
     */
    List<FeeEntity> findByIsActive(Boolean isActive);
    
    /**
     * Tìm tất cả khoản phí đang hoạt động
     */
    List<FeeEntity> findByIsActiveTrue();
    
    /**
     * Tìm tất cả khoản phí không hoạt động
     */
    List<FeeEntity> findByIsActiveFalse();
    
    /**
     * Tìm khoản phí active và sắp xếp theo tên
     */
    List<FeeEntity> findByIsActiveTrueOrderByFeeNameAsc();
    
    /**
     * Tìm khoản phí active và sắp xếp theo ngày tạo
     */
    List<FeeEntity> findByIsActiveTrueOrderByCreatedDateDesc();
    
    // ====================================================
    // Tìm theo ID và active status
    // ====================================================
    
    /**
     * Tìm khoản phí theo ID và chỉ lấy khoản phí đang hoạt động
     */
    Optional<FeeEntity> findByIdAndIsActiveTrue(Long id);
    
    /**
     * Tìm khoản phí theo ID và chỉ lấy khoản phí không hoạt động
     */
    Optional<FeeEntity> findByIdAndIsActiveFalse(Long id);
    
    // ====================================================
    // Count methods
    // ====================================================
    
    /**
     * Đếm số khoản phí đang hoạt động
     */
    long countByIsActiveTrue();
    
    /**
     * Đếm số khoản phí không hoạt động
     */
    long countByIsActiveFalse();
    
    /**
     * Đếm số khoản phí theo tên
     */
    long countByFeeNameContaining(String feeName);
    
    // ====================================================
    // Exists/Validation methods
    // ====================================================
    
    /**
     * Kiểm tra tồn tại theo tên khoản phí
     */
    boolean existsByFeeName(String feeName);
    
    /**
     * Kiểm tra tồn tại theo tên khoản phí (ignore case)
     */
    boolean existsByFeeNameIgnoreCase(String feeName);
    
    /**
     * Kiểm tra tồn tại theo tên khoản phí nhưng loại trừ ID
     */
    boolean existsByFeeNameAndIdNot(String feeName, Long id);
    
    /**
     * Kiểm tra tồn tại theo tên khoản phí (ignore case) nhưng loại trừ ID
     */
    boolean existsByFeeNameIgnoreCaseAndIdNot(String feeName, Long id);
    
    // ====================================================
    // Custom @Query methods (nếu cần logic phức tạp hơn)
    // ====================================================
    
    /**
     * Tìm khoản phí với monthly fee khác null và > 0
     */
    @Query("SELECT f FROM FeeEntity f WHERE f.monthlyFee IS NOT NULL AND f.monthlyFee != '0' AND f.isActive = true")
    List<FeeEntity> findActiveMonthlyFees();
    
    /**
     * Tìm khoản
     */
}