package com.app.client.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityTest {

    @Test
    void hashAndVerifyRoundTrip() {
        String hash = Security.hashPassword("secret123");
        assertTrue(Security.verifyPassword("secret123", hash));
        assertFalse(Security.verifyPassword("wrong", hash));
    }

    @Test
    void hashUsesSalt() {
        String hash1 = Security.hashPassword("same");
        String hash2 = Security.hashPassword("same");
        assertNotEquals(hash1, hash2);
    }
}
