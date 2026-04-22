package com.budget.buoy.authentication;

import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;



@RestController
public class UserController {
    
    private final UserRepository userRepository;
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @GetMapping("/user-info")
    public Map<String, String> getUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user= userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Map<String, String> userDetails=new HashMap<>();
        userDetails.put("username", user.username());
        userDetails.put("email", user.email());
        userDetails.put("fullName", user.fullName());
        userDetails.put("balance", user.balance().toString());
        return userDetails;
    }
    
}
