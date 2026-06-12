package com.budget.buoy.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;

@Service
public class FCMService {

    public void sendNotification(String token, String title, String body) throws FirebaseMessagingException {
        Message message = Message.builder()
            .setToken(token)
            .setNotification(
                Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build()
            )
            .build();

        String response = FirebaseMessaging.getInstance().send(message);
        System.out.println("Notification sent: " + response);
    }

    // Send to multiple devices
    public void sendMulticast(List<String> tokens, String title, String body) throws FirebaseMessagingException {
        MulticastMessage message = MulticastMessage.builder()
            .addAllTokens(tokens)
            .setNotification(
                Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build()
            )
            .build();

        BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
        System.out.println("Success: " + response.getSuccessCount());
        System.out.println("Failed: " + response.getFailureCount());
    }

    // Send with extra data payload
    public void sendWithData(String token, String title, String body, Map<String, String> data) throws FirebaseMessagingException {
        Message message = Message.builder()
            .setToken(token)
            .setNotification(
                Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build()
            )
            .putAllData(data)
            .build();

        FirebaseMessaging.getInstance().send(message);
    }
}
