package com.fx.login.repo;

import com.fx.login.model.PendingUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PendingUserRepo extends JpaRepository<PendingUser, Long> {

    Optional<PendingUser> findByEmail(String email);
    void deleteByEmail(String email);
    boolean existsByEmail(String email);
}
