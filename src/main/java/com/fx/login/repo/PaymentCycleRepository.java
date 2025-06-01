package com.fx.login.repo;

import com.fx.login.model.PaymentCycleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentCycleRepository extends JpaRepository<PaymentCycleEntity, Long> {
    List<PaymentCycleEntity> findByActiveTrue();
    List<PaymentCycleEntity> findByFeeId(Long feeId);
}