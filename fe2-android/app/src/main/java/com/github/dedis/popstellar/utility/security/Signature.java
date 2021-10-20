package com.github.dedis.popstellar.utility.security;

import android.util.Log;

import com.google.crypto.tink.PublicKeySign;
import com.google.crypto.tink.PublicKeyVerify;
import com.google.crypto.tink.subtle.Ed25519Verify;

import java.security.GeneralSecurityException;
import java.util.Base64;

public class Signature {

  public static final String TAG = Signature.class.getSimpleName();

  private Signature() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Helper method to verify signature of a message
   *
   * @param messageId Base 64 URL encode Id of the message to sign
   * @param senderPkBuf Base 64 decoded public key of the signer
   * @param signatureBuf Base 64 URL decoded signature of the signer
   * @return false if there was a problem signing the message
   */
  public static boolean verifySignature(String messageId, byte[] senderPkBuf, byte[] signatureBuf) {
    try {
      PublicKeyVerify verifier = new Ed25519Verify(senderPkBuf);
      verifier.verify(signatureBuf, Base64.getUrlDecoder().decode(messageId));
    } catch (GeneralSecurityException e) {
      Log.d(TAG, "failed to verify witness signature " + e.getMessage());
      return false;
    }
    return true;
  }

  /**
   * Helper method to generate signature of a message
   *
   * @param signer public key of witness signing the message
   * @param messageIdBuf Base 64 URL decoded message ID
   * @return the String signature of the messageIdBuf
   */
  public static String generateSignature(PublicKeySign signer, byte[] messageIdBuf) {
    String signature = null;

    try {
      signature = Base64.getUrlEncoder().encodeToString(signer.sign(messageIdBuf));
    } catch (GeneralSecurityException e) {
      Log.d(TAG, "failed to generate signature", e);
    }
    return signature;
  }
}
