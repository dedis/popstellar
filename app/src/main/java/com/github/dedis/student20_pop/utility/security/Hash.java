package com.github.dedis.student20_pop.utility.security;

import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.StringJoiner;

/**
 * SHA256 Hashing Class
 */
public class Hash {

    public static final String TAG = Hash.class.getSimpleName();

    private static final char DELIMITER = '\"';

    /**
     * Hash some objects using SHA256.
     * Concatenate the object's string representation following the protocol's directive.
     * Then hash the obtained string
     *
     * @param data : the objects to hash
     * @return the hashed data or null if failed to hash
     *
     * @throws IllegalArgumentException if the data is null
     */
    public static String hash(Object... data) {
        if (data == null) {
            throw new IllegalArgumentException("Can't hash a null data");
        }

        StringJoiner joiner = new StringJoiner(",", "[","]");
        for(Object elem : data)
            joiner.add(DELIMITER + esc(elem.toString()) + DELIMITER);

        return hash(joiner.toString());
    }

    private static String esc(String input) {
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"");
    }

    /**
     * Hash a data using SHA256.
     *
     * @param data to hash
     * @return the hashed data or null if failed to hash
     * @throws IllegalArgumentException if the data is null
     */
    protected static String hash(String data) {
        if (data == null)
            throw new IllegalArgumentException("Can't hash a null data");

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            Log.e(Hash.TAG, "Failed to hash data", e);
            e.printStackTrace();
            return null;
        }
    }
}
