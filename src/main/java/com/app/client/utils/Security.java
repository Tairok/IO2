package com.app.client.utils;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Utility class for hashing and verifying passwords using at.favre.lib BCrypt.
 */
public class Security {

    /**
     * Hashes a plain-text password using BCrypt.
     *
     * @param plain plain-text password
     * @return BCrypt hash string
     */
    public static String hashPassword(String plain) {
        return BCrypt.withDefaults()
                .hashToString(12, plain.toCharArray());
    }

    /**
     * Verifies a plain‐text password against a stored BCrypt hash.
     *
     * @param plain the raw password to check
     * @param hash  the stored BCrypt hash
     * @return true if they match, false otherwise
     */
    public static boolean verifyPassword(String plain, String hash) {
        BCrypt.Result result = BCrypt.verifyer()
                .verify(plain.toCharArray(), hash);
        return result.verified;
    }
}

