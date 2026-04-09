package com.app.client.utils;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Utility class for hashing and verifying passwords using at.favre.lib BCrypt.
 */
public class Security {
    public static String hashPassword(String plain) {
        // cost factor 12 is a good balance of security and performance
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

