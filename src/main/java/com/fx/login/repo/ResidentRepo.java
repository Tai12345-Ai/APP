package com.fx.login.repo;
import com.fx.login.model.Resident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResidentRepo extends JpaRepository<Resident, Long> {
    // Các phương thức truy vấn
}