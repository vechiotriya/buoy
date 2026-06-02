package com.budget.buoy.authentication;

public record InstagramTokenResponse(
        String access_token,
        String user_id) {
}
