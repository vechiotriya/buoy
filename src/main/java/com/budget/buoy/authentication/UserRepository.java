package com.budget.buoy.authentication;

import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.web.bind.annotation.RestController;

@RestController
public interface UserRepository extends ListCrudRepository<User, Number> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByUsername(String username);
    User save(User user);
}
