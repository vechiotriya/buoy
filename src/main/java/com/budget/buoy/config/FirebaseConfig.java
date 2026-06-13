package com.budget.buoy.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

@Configuration
public class FirebaseConfig {

@Bean
public FirebaseApp firebaseApp() throws IOException {
    if (FirebaseApp.getApps().isEmpty()) {
        File file = new File("/etc/secrets/buoy-7cadf-firebase-adminsdk-fbsvc-a484d9e799.json");

        FileInputStream serviceAccount = new FileInputStream(file);

        GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);

        FirebaseOptions options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .build();

        return FirebaseApp.initializeApp(options);
    }
    return FirebaseApp.getInstance();
}
}
