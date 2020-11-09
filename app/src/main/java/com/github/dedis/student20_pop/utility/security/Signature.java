package com.github.dedis.student20_pop.utility.security;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.crypto.tink.subtle.Ed25519Sign;
import com.google.crypto.tink.subtle.Hex;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * ED25519 Signing Class
 */
public class Signature {

    public static final String TAG = Signature.class.getSimpleName();

    /**
     * Sign a data using ED25519 Signature
     *
     * @param privateKey used to sign
     * @param data to sign, not hashed
     * @return the signature or null if failed to sign
     * @throws IllegalArgumentException if any parameter is null
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String sign(String privateKey, String data) throws IllegalArgumentException {
        if(privateKey == null || data == null) {
            throw new IllegalArgumentException("Can't sign a null data");
        }
        String signature = null;
        try {
            byte[] hash = Base64.getDecoder().decode(Hash.hash(data));
            Ed25519Sign signer = new Ed25519Sign(Hex.decode(privateKey));
            signature =  Base64.getEncoder().encodeToString(signer.sign(hash));
        } catch (GeneralSecurityException e) {
            Log.e(TAG, "Failed to sign the data", e);
            e.printStackTrace();
        }
        return signature;
    }

    /**
     * Sign a list of data using ED25519 Signature
     *
     * @param privateKeys used to sign
     * @param data to sign, not hashed
     * @return the list of signatures or null if failed to sign
     * @throws IllegalArgumentException if any parameter is null (including one of the private keys)
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static ArrayList<String> sign(List<String> privateKeys, String data) throws IllegalArgumentException {
        if(privateKeys == null || privateKeys.contains(null) || data == null) {
            throw new IllegalArgumentException("Can't sign a null data");
        }
        ArrayList<String> signature = new ArrayList<>();
        for(String privateKey : privateKeys) {
            signature.add(Signature.sign(privateKey, data));
        }
        return signature;
    }
}
