package com.github.dedis.student20_pop.utility.security;

import android.util.Log;
import com.google.crypto.tink.subtle.Ed25519Sign;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** ED25519 Signing Class */
@Deprecated
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
  public static String sign(String privateKey, String data) {
    if (privateKey == null || data == null) {
      throw new IllegalArgumentException("Can't sign a null data");
    }
    String signature = null;
    try {
      byte[] dataByte = Base64.getDecoder().decode(data);
      Ed25519Sign signer = new Ed25519Sign(Base64.getDecoder().decode(privateKey));
      signature = Base64.getEncoder().encodeToString(signer.sign(dataByte));
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
  public static ArrayList<String> sign(List<String> privateKeys, String data) {
    if (privateKeys == null || privateKeys.contains(null) || data == null) {
      throw new IllegalArgumentException("Can't sign a null data");
    }
    ArrayList<String> signature = new ArrayList<>();
    for (String privateKey : privateKeys) {
      signature.add(Signature.sign(privateKey, data));
    }
    return signature;
  }
}
