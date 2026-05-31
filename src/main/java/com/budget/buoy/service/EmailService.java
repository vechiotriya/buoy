package com.budget.buoy.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class EmailService {

    private final WebClient webClient;

    @Value("${BREVO_API_KEY}")
    private String apiKey;

    @Value("${BREVO_SENDER_EMAIL}")
    private String senderEmail;

    @Value("${BREVO_SENDER_NAME:Buoy}")
    private String senderName;

    public EmailService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.brevo.com/v3")
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
                .uri("/smtp/email")
                .header("api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "sender", Map.of(
                                "email", senderEmail,
                                "name", senderName
                        ),
                        "to", List.of(Map.of(
                                "email", to,
                                "name", name
                        )),
                        "subject", "Buoy - Password Reset OTP",
                        "htmlContent", html
                ))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}