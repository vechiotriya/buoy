package com.budget.buoy.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.budget.buoy.exception.EmailDeliveryException;

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
    String html = """
            <h2>Password Reset</h2>
            <p>Hi %s,</p>
            <p>Your OTP is: <strong>%s</strong></p>
            <p>Expires in 10 minutes.</p>
            """.formatted(name, otp);

    var body = Map.of(
            "sender",      Map.of("email", senderEmail, "name", senderName),
            "to",          List.of(Map.of("email", to, "name", name)),
            "subject",     "Buoy - Password Reset OTP",
            "htmlContent", html
    );

    try {
        restClient.post()
                .uri("/smtp/email")
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    String responseBody = new String(res.getBody().readAllBytes());
                    throw new EmailDeliveryException(
                            "Brevo error " + res.getStatusCode() + ": " + responseBody);
                })
                .toBodilessEntity();

    } catch (EmailDeliveryException e) {
        log.error("Email delivery failed for [{}]: {}", to, e.getMessage());
        throw e; // re-throw so the caller (e.g. your auth flow) can respond with 502
    } catch (Exception e) {
        log.error("Unexpected error sending email to [{}]", to, e); // no {}, logs full trace
        throw new EmailDeliveryException("Unexpected email error", e);
    }
}

}