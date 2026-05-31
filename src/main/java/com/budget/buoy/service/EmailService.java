package com.budget.buoy.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class EmailService {

        private final RestClient restClient;
        private final String senderEmail;
        private final String senderName;
        private static final Logger log = LoggerFactory.getLogger(EmailService.class);

        public EmailService(
                        @Value("${BREVO_API_KEY}") String apiKey,
                        @Value("${BREVO_SENDER_EMAIL}") String senderEmail,
                        @Value("${BREVO_SENDER_NAME:Buoy}") String senderName) {

                this.senderEmail = senderEmail;
                this.senderName = senderName;
                this.restClient = RestClient.builder()
                                .baseUrl("https://api.brevo.com/v3")
                                .defaultHeader("api-key", apiKey)
                                .defaultHeader("Content-Type", "application/json")
                                .build();
        }

        public void sendOtp(String to, String name, String otp) {
                log.info("Sending email to {} with OTP {}", to, otp);
                String html = """
                                <h2>Password Reset</h2>
                                <p>Hi %s,</p>
                                <p>Your OTP is: <strong>%s</strong></p>
                                <p>Expires in 10 minutes.</p>
                                """.formatted(name, otp);

                var body = Map.of(
                                "sender", Map.of("email", senderEmail, "name", senderName),
                                "to", List.of(Map.of("email", to, "name", name)),
                                "subject", "Buoy - Password Reset OTP",
                                "htmlContent", html);

                restClient.post()
                                .uri("/smtp/email")
                                .body(body)
                                .retrieve()
                                .onStatus(HttpStatusCode::isError, (req, res) -> {
                                        log.error("Error sending email: {}", res);
                                })
                                .toBodilessEntity();
        }
}