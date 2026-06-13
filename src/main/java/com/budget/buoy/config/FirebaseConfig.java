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
        System.out.println("File exists: " + file.exists());
        System.out.println("File size: " + file.length());

        FileInputStream serviceAccount = new FileInputStream(file);

        GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
        System.out.println("Credentials loaded: " + credentials); // 👈 confirm credentials loaded

        FirebaseOptions options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .build();

        return FirebaseApp.initializeApp(options);
    }
    return FirebaseApp.getInstance();
}
}
