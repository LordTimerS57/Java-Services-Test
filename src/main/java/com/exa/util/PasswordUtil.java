package com.exa.rest;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Hashes passwords without storing them in clear text. */
public final class PasswordUtil {
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    private PasswordUtil() { }

    public static String hash(String password) {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        byte[] derived = derive(password, salt, ITERATIONS);
        return ITERATIONS + ":" + encode(salt) + ":" + encode(derived);
    }

    public static boolean matches(String password, String stored) {
        if (password == null || stored == null || stored.isBlank()) return false;
        if (!stored.contains(":")) return stored.equals(password); // legacy data migration
        try {
            String[] parts = stored.split(":", 3);
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);
            byte[] actual = derive(password, salt, iterations);
            if (actual.length != expected.length) return false;
            int difference = 0;
            for (int i = 0; i < actual.length; i++) difference |= actual[i] ^ expected[i];
            return difference == 0;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception exception) {
            throw new IllegalStateException("Password hashing unavailable", exception);
        }
    }

    private static String encode(byte[] bytes) { return Base64.getEncoder().encodeToString(bytes); }
}
