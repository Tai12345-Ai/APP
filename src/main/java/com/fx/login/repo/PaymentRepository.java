package com.fx.login.repo;

import com.fx.login.model.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    // Tìm payment theo unpaid entity (sửa để dùng relationship thay vì unpaid_id)
    @Query("SELECT p FROM PaymentEntity p WHERE p.unpaid.id = :unpaidId")
    List<PaymentEntity> findByUnpaidId(@Param("unpaidId") Long unpaidId);

    // Tìm payment theo status
    List<PaymentEntity> findByStatus(String status);

    // Tìm payment theo payment method
    List<PaymentEntity> findByPaymentMethod(String paymentMethod);

    // Tìm payment theo người thanh toán
    List<PaymentEntity> findByPaidBy(String paidBy);

    // Tìm payment theo unpaid entity trực tiếp
    @Query("SELECT p FROM PaymentEntity p WHERE p.unpaid = :unpaid")
    List<PaymentEntity> findByUnpaid(@Param("unpaid") com.fx.login.model.UnpaidEntity unpaid);
}