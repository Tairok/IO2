package com.app.server.utils;

/**
 * Utility class for hashing and verifying passwords using at.favre.lib BCrypt.
 */
public class Security {

    /**
     * Verifies a plain‐text password against a stored BCrypt hash.
     *
     * @param plain the raw password to check
     * @param hash  the stored BCrypt hash
     * @return true if they match, false otherwise
     */
    public static boolean verifyPassword(String plain, String hash) {
        return com.app.client.utils.Security.verifyPassword(plain, hash);
    }
}
