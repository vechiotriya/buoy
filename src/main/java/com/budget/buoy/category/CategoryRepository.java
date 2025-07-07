package com.budget.buoy.category;

import java.util.List;

import org.springframework.data.repository.ListCrudRepository;

public interface CategoryRepository extends ListCrudRepository<Category, Integer> {
    List<Category> findByUserId(String userId);
    Category save(Category category);
}