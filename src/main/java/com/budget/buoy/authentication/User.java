package com.budget.buoy.authentication;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Table("users")
public record User(
                @Id String id,
                @NotBlank(message = "Full name is required") String fullName,

                @NotBlank(message = "Username is required") @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters") String username,

                String profile,
                
                @Email(message = "Email must be valid") @NotBlank(message = "Email is required") String email,

                @Nullable @Size(min = 8, message = "Password must be at least 8 characters") String password,

                AuthProvider provider,

                BigDecimal balance,

                BudgetingStyle prefBudgetStyle, 

                @Version Integer version) {

}
