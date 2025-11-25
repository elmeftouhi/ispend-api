package org.example.expenseapi.repository;

import org.example.expenseapi.model.ExpenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExpenseStatusRepository extends JpaRepository<ExpenseStatus, Long> {
    Optional<ExpenseStatus> findByName(String name);

    Optional<ExpenseStatus> findByIsDefaultTrue();

    @Modifying
    @Query("UPDATE ExpenseStatus s SET s.isDefault = false WHERE s.id <> :excludeId AND s.isDefault = true")
    int clearOtherDefaults(@Param("excludeId") Long excludeId);
}
