package com.example.styloflow.repository;

import com.example.styloflow.entity.CashRegister;
import com.example.styloflow.enums.CashRegisterStatus;
import jakarta.persistence.LockModeType;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface CashRegisterRepository extends JpaRepository<CashRegister, Long> {

    @EntityGraph(attributePaths = {"openedBy", "closedBy"})
    Optional<CashRegister> findByStatus(CashRegisterStatus status);

    boolean existsByStatus(CashRegisterStatus status);

    /**
     * Same as {@link #findByStatus} but takes a row lock ({@code SELECT ... FOR UPDATE}), so selling, voiding
     * and closing the register are serialized and no sale slips in while the register is being closed.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CashRegister> findWithLockByStatus(CashRegisterStatus status);

    @NullMarked
    @EntityGraph(attributePaths = {"openedBy", "closedBy"})
    Page<CashRegister> findAll(Pageable pageable);

    @NullMarked
    @EntityGraph(attributePaths = {"openedBy", "closedBy"})
    Optional<CashRegister> findById(Long id);
}
