package com.safewalk.config;

import com.google.firebase.FirebaseApp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FirebaseConfigTest {

    @BeforeEach
    @AfterEach
    void cleanupFirebase() {
        for (FirebaseApp app : FirebaseApp.getApps()) {
            app.delete();
        }
    }

    @Test
    void testDevelopmentFallback_WhenNoCredentials_DoesNotThrow() {
        MockEnvironment devEnv = new MockEnvironment();
        devEnv.setActiveProfiles("dev");

        FirebaseConfig config = new FirebaseConfig(devEnv);
        assertDoesNotThrow(config::initializeFirebase);
        assertTrue(FirebaseApp.getApps().isEmpty(), "FirebaseApp should remain uninitialized in DEV mode when credentials are missing");
    }

    @Test
    void testDevelopmentFallback_WhenCredentialsPathIsDirectory_DoesNotThrow() throws IOException {
        MockEnvironment devEnv = new MockEnvironment();
        devEnv.setActiveProfiles("dev");
        File tempDir = File.createTempFile("fake-firebase-dir", "");
        assertTrue(tempDir.delete());
        assertTrue(tempDir.mkdir());
        tempDir.deleteOnExit();

        devEnv.setProperty("GOOGLE_APPLICATION_CREDENTIALS", tempDir.getAbsolutePath());

        FirebaseConfig config = new FirebaseConfig(devEnv);
        assertDoesNotThrow(config::initializeFirebase);
        assertTrue(FirebaseApp.getApps().isEmpty());
    }

    @Test
    void testDevelopmentFallback_WhenCredentialsPathDoesNotExist_DoesNotThrow() {
        MockEnvironment devEnv = new MockEnvironment();
        devEnv.setActiveProfiles("dev");
        devEnv.setProperty("GOOGLE_APPLICATION_CREDENTIALS", "/non/existent/path/firebase.json");

        FirebaseConfig config = new FirebaseConfig(devEnv);
        assertDoesNotThrow(config::initializeFirebase);
        assertTrue(FirebaseApp.getApps().isEmpty());
    }

    @Test
    void testProductionFailure_WhenCredentialsMissing_ThrowsIllegalStateException() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");

        FirebaseConfig config = new FirebaseConfig(prodEnv);
        IllegalStateException exception = assertThrows(IllegalStateException.class, config::initializeFirebase);
        assertTrue(exception.getMessage().contains("PRODUCTION"), "Exception should mention PRODUCTION error");
    }

    @Test
    void testProductionFailure_WhenCredentialsPathDoesNotExist_ThrowsIllegalStateException() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");
        prodEnv.setProperty("GOOGLE_APPLICATION_CREDENTIALS", "/non/existent/path/firebase.json");

        FirebaseConfig config = new FirebaseConfig(prodEnv);
        IllegalStateException exception = assertThrows(IllegalStateException.class, config::initializeFirebase);
        assertTrue(exception.getMessage().contains("PRODUCTION"));
    }

    @Test
    void testProductionFailure_WhenStrictModeEnabled_ThrowsIllegalStateException() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("safewalk.firebase.strict", "true");

        FirebaseConfig config = new FirebaseConfig(env);
        assertTrue(config.isProductionEnvironment(), "Should be considered production/strict");
        assertThrows(IllegalStateException.class, config::initializeFirebase);
    }

    @Test
    void testIsProductionEnvironment_ProfileVariations() {
        MockEnvironment envDefault = new MockEnvironment();
        assertFalse(new FirebaseConfig(envDefault).isProductionEnvironment());

        MockEnvironment envDev = new MockEnvironment();
        devEnvWithProfile(envDev, "dev");
        assertFalse(new FirebaseConfig(envDev).isProductionEnvironment());

        MockEnvironment envProd = new MockEnvironment();
        devEnvWithProfile(envProd, "prod");
        assertTrue(new FirebaseConfig(envProd).isProductionEnvironment());

        MockEnvironment envProduction = new MockEnvironment();
        devEnvWithProfile(envProduction, "production");
        assertTrue(new FirebaseConfig(envProduction).isProductionEnvironment());

        MockEnvironment envAppEnv = new MockEnvironment();
        envAppEnv.setProperty("app.env", "prod");
        assertTrue(new FirebaseConfig(envAppEnv).isProductionEnvironment());
    }

    @Test
    void testIsProductionEnvironment_EnvironmentVariableDetection() {
        // Test ENVIRONMENT=production
        MockEnvironment envProd1 = new MockEnvironment();
        envProd1.setProperty("ENVIRONMENT", "production");
        assertTrue(new FirebaseConfig(envProd1).isProductionEnvironment());

        // Test ENVIRONMENT=prod
        MockEnvironment envProd2 = new MockEnvironment();
        envProd2.setProperty("ENVIRONMENT", "prod");
        assertTrue(new FirebaseConfig(envProd2).isProductionEnvironment());

        // Test ENVIRONMENT=development
        MockEnvironment envDev = new MockEnvironment();
        envDev.setProperty("ENVIRONMENT", "development");
        assertFalse(new FirebaseConfig(envDev).isProductionEnvironment());
    }

    @Test
    void testIsProductionEnvironment_AppEnvVariableDetection() {
        // Test APP_ENV=production
        MockEnvironment envProd1 = new MockEnvironment();
        envProd1.setProperty("APP_ENV", "production");
        assertTrue(new FirebaseConfig(envProd1).isProductionEnvironment());

        // Test APP_ENV=prod
        MockEnvironment envProd2 = new MockEnvironment();
        envProd2.setProperty("APP_ENV", "prod");
        assertTrue(new FirebaseConfig(envProd2).isProductionEnvironment());

        // Test APP_ENV=staging/dev
        MockEnvironment envDev = new MockEnvironment();
        envDev.setProperty("APP_ENV", "local");
        assertFalse(new FirebaseConfig(envDev).isProductionEnvironment());
    }

    @Test
    void testIsProductionEnvironment_FirebaseStrictVariableDetection() {
        // Test FIREBASE_STRICT=true
        MockEnvironment envStrict1 = new MockEnvironment();
        envStrict1.setProperty("FIREBASE_STRICT", "true");
        assertTrue(new FirebaseConfig(envStrict1).isProductionEnvironment());

        // Test FIREBASE_STRICT=1
        MockEnvironment envStrict2 = new MockEnvironment();
        envStrict2.setProperty("FIREBASE_STRICT", "1");
        assertTrue(new FirebaseConfig(envStrict2).isProductionEnvironment());

        // Test FIREBASE_STRICT=false
        MockEnvironment envStrict3 = new MockEnvironment();
        envStrict3.setProperty("FIREBASE_STRICT", "false");
        assertFalse(new FirebaseConfig(envStrict3).isProductionEnvironment());
    }

    @Test
    void testIsProductionEnvironment_DirectOsEnvSimulationWithoutSpringEnv() {
        Map<String, String> simulatedOsEnv = new HashMap<>();
        simulatedOsEnv.put("ENVIRONMENT", "production");

        FirebaseConfig config = new FirebaseConfig(null) {
            @Override
            protected String resolveConfigValue(String key) {
                return simulatedOsEnv.get(key);
            }
        };
        assertTrue(config.isProductionEnvironment(), "Should detect ENVIRONMENT=production via OS env when Spring env is null");

        simulatedOsEnv.clear();
        simulatedOsEnv.put("APP_ENV", "prod");
        assertTrue(config.isProductionEnvironment(), "Should detect APP_ENV=prod via OS env when Spring env is null");

        simulatedOsEnv.clear();
        simulatedOsEnv.put("FIREBASE_STRICT", "true");
        assertTrue(config.isProductionEnvironment(), "Should detect FIREBASE_STRICT=true via OS env when Spring env is null");

        simulatedOsEnv.clear();
        simulatedOsEnv.put("ENVIRONMENT", "dev");
        assertFalse(config.isProductionEnvironment(), "Should evaluate false for dev via OS env when Spring env is null");
    }

    private void devEnvWithProfile(MockEnvironment env, String profile) {
        env.setActiveProfiles(profile);
    }
}
