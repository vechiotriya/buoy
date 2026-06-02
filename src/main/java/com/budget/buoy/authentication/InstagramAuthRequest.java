package com.budget.buoy.authentication;

public record InstagramAuthRequest(
        String code,
        Double balance) {
}