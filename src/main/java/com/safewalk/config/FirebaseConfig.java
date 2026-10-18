package com.safewalk.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @PostConstruct
    public void initializeFirebase() {
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("Firebase App is already initialized.");
            return;
        }

        String googleCredentialsEnv = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (googleCredentialsEnv == null || googleCredentialsEnv.trim().isEmpty()) {
            log.warn("GOOGLE_APPLICATION_CREDENTIALS environment variable is not set. Skipping Firebase Admin SDK initialization (running in DEV/Simulation mode).");
            return;
        }

        try {
            java.io.File credsFile = new java.io.File(googleCredentialsEnv.trim());
            GoogleCredentials credentials;
            if (credsFile.exists() && credsFile.isFile()) {
                log.info("Loading Firebase credentials from file path: {}", credsFile.getAbsolutePath());
                try (java.io.InputStream is = new java.io.FileInputStream(credsFile)) {
                    credentials = GoogleCredentials.fromStream(is);
                }
            } else {
                credentials = GoogleCredentials.getApplicationDefault();
            }

            FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                    .setCredentials(credentials);
            if (credentials instanceof com.google.auth.oauth2.ServiceAccountCredentials) {
                String pId = ((com.google.auth.oauth2.ServiceAccountCredentials) credentials).getProjectId();
                if (pId != null && !pId.trim().isEmpty()) {
                    optionsBuilder.setProjectId(pId.trim());
                }
            }

            FirebaseOptions options = optionsBuilder.build();
            FirebaseApp.initializeApp(options);
            log.info("Successfully initialized Firebase App [projectId={}]", options.getProjectId());
        } catch (IOException e) {
            String errorMsg = "FCM Configuration Error: Failed to load credentials from GOOGLE_APPLICATION_CREDENTIALS: " + e.getMessage();
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg, e);
        }
    }

    private boolean isTestEnvironment() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (element.getClassName().startsWith("org.junit.") || element.getClassName().startsWith("org.springframework.test.")) {
                return true;
            }
        }
        return false;
    }
}
