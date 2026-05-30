package com.budget.buoy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.budget.buoy.authentication.AuthController;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String to, String name, String otp) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject("Buoy – Your password reset code");
            msg.setText("""
                    Hi %s,

                    Your one-time password reset code is:

                        %s

                    It expires in 10 minutes. If you didn't request this, ignore this email.

                    – The Budget Buoy team
                    """.formatted(name, otp));
            mailSender.send(msg);
            log.info("Email sent to {}",to);
        } catch (Exception e) {
            log.error("Failed to send email", e);
        }

    }
}