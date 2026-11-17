package com.covoiturage.service;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PasswordHashing {
    private static final String PREFIX = "{pbkdf2-sha256}";
    private static final int ITERATIONS = 600_000;
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;

    private final SecureRandom random = new SecureRandom();

    public String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        Base64.Encoder encoder = Base64.getEncoder();
        return PREFIX + ITERATIONS + "$" + encoder.encodeToString(salt)
                + "$" + encoder.encodeToString(deriveKey(password, salt, ITERATIONS));
    }

    public boolean isHashed(String storedPassword) {
        return storedPassword != null && storedPassword.startsWith(PREFIX);
    }

    public boolean matches(String password, String storedPassword) {
        if (storedPassword == null) return false;
        if (!isHashed(storedPassword)) {
            // Upgrade the legacy cleartext value only after a successful login.
            return MessageDigest.isEqual(storedPassword.getBytes(StandardCharsets.UTF_8),
                    password.getBytes(StandardCharsets.UTF_8));
        }

        String[] parts = storedPassword.substring(PREFIX.length()).split("\\$", -1);
        if (parts.length != 3) return false;
        try {
            int iterations = Integer.parseInt(parts[0]);
            if (iterations < 1 || iterations > ITERATIONS) return false;
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] salt = decoder.decode(parts[1]);
            byte[] expectedKey = decoder.decode(parts[2]);
            if (salt.length != SALT_BYTES || expectedKey.length != KEY_BITS / Byte.SIZE) return false;
            return MessageDigest.isEqual(expectedKey, deriveKey(password, salt, iterations));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private byte[] deriveKey(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("PBKDF2WithHmacSHA256 indisponible.", ex);
        } finally {
            spec.clearPassword();
        }
    }
}
