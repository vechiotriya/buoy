package com.budget.buoy.transaction;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

public interface TransactionRepository extends ListCrudRepository<Transaction, Integer> {
    // This interface extends ListCrudRepository, which provides CRUD operations for the Transaction entity.    
    List<Transaction> findByEmail(String email);
    long count();
    Optional<Transaction> findById(Integer id);
    Transaction save(Transaction transaction);
    void deleteById(Integer id);
    List<Transaction> findByCategory(String category);
}
