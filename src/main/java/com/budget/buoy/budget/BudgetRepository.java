package com.budget.buoy.budget;

import java.util.List;

import org.springframework.data.repository.ListCrudRepository;

public interface BudgetRepository extends ListCrudRepository<Budget, String> {
    List<Budget> findByUserId(String userId);
    Budget save(Budget budget);
    void deleteById(String id);
}
