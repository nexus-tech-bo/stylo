package com.example.styloflow.repository;

import com.example.styloflow.entity.PlatformAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlatformAdminRepository extends JpaRepository<PlatformAdmin, Long> {
    Optional<PlatformAdmin> findByUsername(String username);

    boolean existsByUsername(String username);
}
