package com.safewalk.config;

import com.google.firebase.FirebaseApp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
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

    private String generateDummyServiceAccountJson() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair kp = kpg.generateKeyPair();
            String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n" +
                    Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(kp.getPrivate().getEncoded()) +
                    "\n-----END PRIVATE KEY-----\n";

            return "{\n" +
                    "  \"type\": \"service_account\",\n" +
                    "  \"project_id\": \"test-safewalk-project\",\n" +
                    "  \"private_key_id\": \"dummy-key-id\",\n" +
                    "  \"private_key\": \"" + privateKeyPem.replace("\n", "\\n") + "\",\n" +
                    "  \"client_email\": \"firebase-adminsdk@test-safewalk-project.iam.gserviceaccount.com\",\n" +
                    "  \"client_id\": \"1234567890\",\n" +
                    "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
                    "  \"token_uri\": \"https://oauth2.googleapis.com/token\"\n" +
                    "}";
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate test RSA key", e);
        }
    }

    @Test
    void testInitializeWithCredentialsJson_InMemorySuccess() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("FIREBASE_CREDENTIALS_JSON", generateDummyServiceAccountJson());

        FirebaseConfig config = new FirebaseConfig(env);
        assertDoesNotThrow(config::initializeFirebase);

        assertFalse(FirebaseApp.getApps().isEmpty(), "FirebaseApp should be initialized from in-memory JSON");
        assertEquals("test-safewalk-project", FirebaseApp.getInstance().getOptions().getProjectId());

        // Verify duplicate initialization safety
        assertDoesNotThrow(config::initializeFirebase, "Subsequent initialization should safely reuse existing app");
        assertEquals(1, FirebaseApp.getApps().size());
    }

    @Test
    void testInitializeWithCredentialsFile_ExistingFileSuccess() throws IOException {
        MockEnvironment env = new MockEnvironment();
        File tempFile = File.createTempFile("test-firebase-creds", ".json");
        tempFile.deleteOnExit();
        Files.writeString(tempFile.toPath(), generateDummyServiceAccountJson());

        env.setProperty("GOOGLE_APPLICATION_CREDENTIALS", tempFile.getAbsolutePath());

        FirebaseConfig config = new FirebaseConfig(env);
        assertDoesNotThrow(config::initializeFirebase);

        assertFalse(FirebaseApp.getApps().isEmpty(), "FirebaseApp should be initialized from credentials file");
        assertEquals("test-safewalk-project", FirebaseApp.getInstance().getOptions().getProjectId());
    }

    @Test
    void testFallbackToCredentialsJson_WhenCredentialsPathPointsToNonExistentFile() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("GOOGLE_APPLICATION_CREDENTIALS", "/non/existent/firebase-service-account.json");
        env.setProperty("FIREBASE_CREDENTIALS_JSON", generateDummyServiceAccountJson());

        FirebaseConfig config = new FirebaseConfig(env);
        assertDoesNotThrow(config::initializeFirebase);

        assertFalse(FirebaseApp.getApps().isEmpty(), "Should fall back to FIREBASE_CREDENTIALS_JSON when file does not exist");
        assertEquals("test-safewalk-project", FirebaseApp.getInstance().getOptions().getProjectId());
    }

    @Test
    void testCredentialsJson_WhenAbsent_PreservesDevFallback() {
        MockEnvironment devEnv = new MockEnvironment();
        devEnv.setActiveProfiles("dev");

        FirebaseConfig config = new FirebaseConfig(devEnv);
        assertDoesNotThrow(config::initializeFirebase);
        assertTrue(FirebaseApp.getApps().isEmpty(), "Should remain in simulation mode when JSON is absent in DEV");
    }

    @Test
    void testCredentialsJson_MalformedInProduction_ThrowsSanitizedException() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");
        prodEnv.setProperty("FIREBASE_CREDENTIALS_JSON", "{invalid-malformed-json-payload}");

        FirebaseConfig config = new FirebaseConfig(prodEnv);
        IllegalStateException exception = assertThrows(IllegalStateException.class, config::initializeFirebase);
        assertTrue(exception.getMessage().contains("FIREBASE_CREDENTIALS_JSON"), "Exception should mention FIREBASE_CREDENTIALS_JSON");
        assertFalse(exception.getMessage().contains("{invalid-malformed-json-payload}"), "Exception message should never leak payload content");
    }

    @Test
    void testCredentialsJson_MalformedInDev_DoesNotThrow() {
        MockEnvironment devEnv = new MockEnvironment();
        devEnv.setActiveProfiles("dev");
        devEnv.setProperty("FIREBASE_CREDENTIALS_JSON", "{invalid-malformed-json-payload}");

        FirebaseConfig config = new FirebaseConfig(devEnv);
        assertDoesNotThrow(config::initializeFirebase);
        assertTrue(FirebaseApp.getApps().isEmpty(), "Should remain in simulation mode on malformed JSON in DEV");
    }
}
