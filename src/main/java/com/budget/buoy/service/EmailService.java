package com.budget.buoy.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class EmailService {

    private final WebClient webClient;

    @Value("${RESEND_API_KEY}")
    private String apiKey;

    public EmailService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.resend.com")
                .build();
    }

    public void sendOtp(String to, String name, String otp) {

        String html = """
            <h2>Password Reset</h2>
            <p>Hi %s,</p>
            <p>Your OTP is:</p>
            <h1>%s</h1>
            <p>Expires in 10 minutes.</p>
            """.formatted(name, otp);

        webClient.post()
                .uri("/emails")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "from", "onboarding@resend.dev",
                        "to", new String[]{to},
                        "subject", "Buoy - Password Reset OTP",
                        "html", html
                ))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}