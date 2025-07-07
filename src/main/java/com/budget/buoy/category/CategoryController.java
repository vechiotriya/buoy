package com.budget.buoy.category;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.budget.buoy.authentication.UserRepository;

import jakarta.validation.Valid;

@RestController
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    // get current logged in user's id
    private String getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"))
                .id();
    }

    public CategoryController(CategoryRepository categoryRepository, UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    // Get all categories for the current user
    @GetMapping("/categories")
    public List<Category> getAllCategories() {
        String userId = getCurrentUser();
        return categoryRepository.findByUserId(userId);
    }

    // Add a new category for the current user
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/categories/add")
    public ResponseEntity<?> addCategory(@Valid @RequestBody Category category) {
        String userId = getCurrentUser();
        if(categoryRepository.existsByNameAndUserId(category.name(), userId)) {
            return ResponseEntity.badRequest().body("Category with this name already exists for the user");
        }
        Category newCategory = new Category(
                NanoIdUtils.randomNanoId(new SecureRandom(), NanoIdUtils.DEFAULT_ALPHABET, 18),
                userId,
                category.name(),
                new BigDecimal(category.budget().doubleValue()).setScale(2, RoundingMode.HALF_UP),
                category.version());
        return ResponseEntity.ok("Category added for: "+newCategory.userId());
    }
}
