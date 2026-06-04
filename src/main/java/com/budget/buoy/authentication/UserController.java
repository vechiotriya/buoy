package com.budget.buoy.authentication;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.budget.buoy.budget.Budget;
import com.budget.buoy.budget.BudgetRepository;
import com.budget.buoy.service.CloudinaryService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
public class UserController {
    private final CloudinaryService cloudinaryService;
    private final UserRepository userRepository;
    private final BudgetRepository budgetRepository;
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    public UserController(UserRepository userRepository, CloudinaryService cloudinaryService,
            BudgetRepository budgetRepository) {
        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
        this.budgetRepository = budgetRepository;
    }

    @GetMapping("/user-info")
    public Map<String, String> getUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Map<String, String> userDetails = new HashMap<>();
        String pfp = user.profile() == null ? "" : user.profile().split(" ")[0];
        userDetails.put("username", user.username());
        userDetails.put("email", user.email());
        userDetails.put("fullName", user.fullName());
        userDetails.put("balance", user.balance().toString());
        userDetails.put("profile", pfp);
        userDetails.put("preferredBudgetStyle",
                user.prefBudgetStyle() == null ? "" : user.prefBudgetStyle().toString());
        return userDetails;
    }

    @PostMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody User body) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        User updatedUser = new User(user.id(), body.fullName(), user.username(), user.profile(), user.email(),
                user.password(),
                user.provider(),
                user.balance(), body.prefBudgetStyle(), user.version());
        log.info("Budget changed", user.prefBudgetStyle(), body.prefBudgetStyle());

        if (user.prefBudgetStyle() != body.prefBudgetStyle()) {
            List<Budget> budgets = budgetRepository.findByUserId(user.id()  );
            budgetRepository.deleteAll(budgets);
            log.info("Budget style changed", user.prefBudgetStyle(), body.prefBudgetStyle());
        }

        userRepository.save(updatedUser);
        return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
    }

    @PostMapping("/profile/picture")
    public ResponseEntity<?> uploadProfilePicture(
            @RequestParam("file") MultipartFile file) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Please select a file to upload."));
        }

        // Optional: enforce max size (e.g. 5 MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "File size must not exceed 5 MB."));
        }

        try {
            Map result = cloudinaryService.uploadProfilePicture(file, user.id());
            User updatedUser = new User(user.id(), user.fullName(), user.username(),
                    result.get("secure_url") + " " + result.get("public_id"), user.email(), user.password(),
                    user.provider(),
                    user.balance(), null, user.version());
            userRepository.save(updatedUser);
            return ResponseEntity.ok(Map.of(
                    "message", "Profile picture uploaded successfully.",
                    "url", result.get("secure_url"), // HTTPS URL
                    "public_id", result.get("public_id") // store this in your DB
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/profile/picture")
    public ResponseEntity<?> deleteProfilePicture() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            if (user.profile() == null)
                return ResponseEntity.ok(Map.of("message", "No profile picture to delete."));
            cloudinaryService.deleteProfilePicture(user.profile().split(" ")[1]);
            User updatedUser = new User(user.id(), user.fullName(), user.username(),
                    null, user.email(), user.password(),
                    user.provider(),
                    user.balance(), null, user.version());
            userRepository.save(updatedUser);
            return ResponseEntity.ok(Map.of("message", "Profile picture deleted."));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Deletion failed: " + e.getMessage()));
        }
    }

}
