package com.budget.buoy.transaction;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends ListCrudRepository<Transaction, String> {
    // This interface extends ListCrudRepository, which provides CRUD operations for
    // the Transaction entity.
    @Query("SELECT * FROM transaction WHERE user_id = :userId")
    List<Transaction> findByUserId(@Param("userId") String userId);
    List<Transaction> findByCategory(String category);

    @Query("SELECT * FROM Transaction WHERE user_id = :userId " +
            "AND transaction_date >= :start AND transaction_date < :end")
    List<Transaction> findByUserIdAndDateBetween(
            @Param("userId") String userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}
