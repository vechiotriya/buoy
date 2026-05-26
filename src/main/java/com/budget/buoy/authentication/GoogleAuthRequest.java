package com.budget.buoy.authentication;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;

public record GoogleAuthRequest(@NotBlank String idToken,BigDecimal balance) {}
