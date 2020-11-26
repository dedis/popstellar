package com.github.dedis.student20_pop.utility.security;

import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * SHA256 Hashing Class
 */
public class Hash {

    public static final String TAG = Hash.class.getSimpleName();

    /**
     * Hash a data using SHA256.
     *
     * @param data to hash
     * @return the hashed data or null if failed to hash
     * @throws IllegalArgumentException if the data is null
     */
    public static String hash(String data) {
        if(data == null) {
            throw new IllegalArgumentException("Can't hash a null data");
        }
        String encoded = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            encoded = Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            Log.e(Hash.TAG, "Failed to hash data", e);
            e.printStackTrace();
        }
        return encoded;
    }
}
