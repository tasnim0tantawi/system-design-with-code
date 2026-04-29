package com.booking.auth.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * POC password hashing — SHA-256 with a fixed-string salt prefix.
 * Production should use BCrypt/Argon2; out of scope for the POC.
 */
@Component
public class PasswordHasher {

    private static final String SALT = "booking-poc-salt::";

    public String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((SALT + password).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean matches(String password, String hashed) {
        return hash(password).equals(hashed);
    }
}
