package com.fx.login.repo;

import com.fx.login.model.UnpaidEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnpaidRepo extends JpaRepository<UnpaidEntity, Long> {
    List<UnpaidEntity> findByStatus(String status);
    List<UnpaidEntity> findByFeeID(Long feeID);

}
