package com.safewalk.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    private final Environment environment;

    @Autowired
    public FirebaseConfig(Environment environment) {
        this.environment = environment;
    }

    public FirebaseConfig() {
        this.environment = null;
    }

    @PostConstruct
    public void initializeFirebase() {
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("Firebase App is already initialized.");
            return;
        }

        boolean isProd = isProductionEnvironment();
        String googleCredentialsEnv = resolveCredentialsPath();

        if (googleCredentialsEnv == null || googleCredentialsEnv.trim().isEmpty()) {
            if (isProd) {
                log.info("Production environment: GOOGLE_APPLICATION_CREDENTIALS not explicitly configured. Attempting Application Default Credentials (ADC)...");
                try {
                    GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
                    initializeWithCredentials(credentials);
                    return;
                } catch (IOException e) {
                    String errorMsg = "FCM Configuration Error in PRODUCTION: Firebase Admin SDK credentials are required but unavailable (GOOGLE_APPLICATION_CREDENTIALS is not set and Application Default Credentials failed): " + e.getMessage();
                    log.error(errorMsg);
                    throw new IllegalStateException(errorMsg, e);
                }
            } else {
                log.warn("GOOGLE_APPLICATION_CREDENTIALS environment variable is not set. Skipping Firebase Admin SDK initialization (running in DEV/Simulation mode).");
                return;
            }
        }

        File credsFile = new File(googleCredentialsEnv.trim());
        GoogleCredentials credentials;

        if (credsFile.exists() && credsFile.isFile()) {
            log.info("Loading Firebase credentials from file path: {}", credsFile.getAbsolutePath());
            try (InputStream is = new FileInputStream(credsFile)) {
                credentials = GoogleCredentials.fromStream(is);
            } catch (IOException e) {
                String errorMsg = "FCM Configuration Error: Failed to read credentials from file '" + credsFile.getAbsolutePath() + "': " + e.getMessage();
                log.error(errorMsg);
                if (isProd) {
                    throw new IllegalStateException(errorMsg, e);
                } else {
                    log.warn("Skipping Firebase Admin SDK initialization due to read error (running in DEV/Simulation mode): {}", e.getMessage());
                    return;
                }
            }
        } else {
            if (isProd) {
                log.warn("Production environment: Credentials file path '{}' does not exist or is a directory. Attempting Application Default Credentials fallback.", googleCredentialsEnv);
                try {
                    credentials = GoogleCredentials.getApplicationDefault();
                } catch (IOException e) {
                    String errorMsg = "FCM Configuration Error in PRODUCTION: Firebase credentials file not found at '" + googleCredentialsEnv + "' and Application Default Credentials unavailable: " + e.getMessage();
                    log.error(errorMsg);
                    throw new IllegalStateException(errorMsg, e);
                }
            } else {
                log.warn("GOOGLE_APPLICATION_CREDENTIALS specified ('{}') but file does not exist or is a directory. Attempting Application Default Credentials fallback.", googleCredentialsEnv);
                try {
                    credentials = GoogleCredentials.getApplicationDefault();
                } catch (IOException e) {
                    log.warn("Application Default Credentials not available ({}). Skipping Firebase Admin SDK initialization (running in DEV/Simulation mode).", e.getMessage());
                    return;
                }
            }
        }

        try {
            initializeWithCredentials(credentials);
        } catch (Exception e) {
            String errorMsg = "FCM Configuration Error: Failed to initialize FirebaseApp with credentials: " + e.getMessage();
            log.error(errorMsg);
            if (isProd) {
                throw new IllegalStateException(errorMsg, e);
            } else {
                log.warn("Skipping Firebase Admin SDK initialization due to error (running in DEV/Simulation mode): {}", e.getMessage());
            }
        }
    }

    private void initializeWithCredentials(GoogleCredentials credentials) {
        FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder().setCredentials(credentials);
        if (credentials instanceof com.google.auth.oauth2.ServiceAccountCredentials) {
            String pId = ((com.google.auth.oauth2.ServiceAccountCredentials) credentials).getProjectId();
            if (pId != null && !pId.trim().isEmpty()) {
                optionsBuilder.setProjectId(pId.trim());
            }
        }

        FirebaseOptions options = optionsBuilder.build();
        FirebaseApp.initializeApp(options);
        log.info("Successfully initialized Firebase App [projectId={}]", options.getProjectId());
    }

    private String resolveCredentialsPath() {
        String path = resolveConfigValue("GOOGLE_APPLICATION_CREDENTIALS");
        if (path == null || path.trim().isEmpty()) {
            path = resolveConfigValue("safewalk.firebase.credentials-path");
        }
        return path != null && !path.trim().isEmpty() ? path.trim() : null;
    }

    public boolean isProductionEnvironment() {
        if (environment != null && environment.acceptsProfiles(Profiles.of("prod", "production"))) {
            return true;
        }

        String envValue = resolveConfigValue("ENVIRONMENT");
        if (isProdString(envValue)) {
            return true;
        }

        String appEnv = resolveConfigValue("APP_ENV");
        if (!isProdString(appEnv)) {
            appEnv = resolveConfigValue("app.env");
        }
        if (isProdString(appEnv)) {
            return true;
        }

        String strict = resolveConfigValue("FIREBASE_STRICT");
        if (strict == null || strict.isEmpty()) {
            strict = resolveConfigValue("safewalk.firebase.strict");
        }
        return "true".equalsIgnoreCase(strict) || "1".equals(strict);
    }

    private boolean isProdString(String value) {
        return value != null && ("prod".equalsIgnoreCase(value.trim()) || "production".equalsIgnoreCase(value.trim()));
    }

    protected String resolveConfigValue(String key) {
        if (environment != null) {
            String val = environment.getProperty(key);
            if (val != null && !val.trim().isEmpty()) {
                return val.trim();
            }
        }
        String sysVal = System.getenv(key);
        return sysVal != null && !sysVal.trim().isEmpty() ? sysVal.trim() : null;
    }
}
