package com.howners.gestion.repository;

import com.howners.gestion.domain.expense.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    List<Expense> findByPropertyId(UUID propertyId);

    List<Expense> findByRentalId(UUID rentalId);

    @Query("SELECT e FROM Expense e WHERE e.property.owner.id = :ownerId ORDER BY e.expenseDate DESC")
    List<Expense> findByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.property.owner.id = :ownerId AND e.expenseDate >= :from AND e.expenseDate < :to")
    BigDecimal sumExpensesByOwnerAndPeriod(@Param("ownerId") UUID ownerId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT e.category, SUM(e.amount) FROM Expense e WHERE e.property.id = :propertyId GROUP BY e.category")
    List<Object[]> sumExpensesByPropertyGroupedByCategory(@Param("propertyId") UUID propertyId);
}
