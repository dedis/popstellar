package com.github.dedis.popstellar.model.objects.security;

import android.util.Log;

import com.google.crypto.tink.PublicKeyVerify;
import com.google.crypto.tink.subtle.Ed25519Verify;

import java.security.GeneralSecurityException;

/** A public key that can be used to verify a signature */
public class PublicKey extends Base64URLData {

  private static final String TAG = PublicKey.class.getSimpleName();

  private final PublicKeyVerify verifier;

  public PublicKey(byte[] data) {
    super(data);
    verifier = new Ed25519Verify(data);
  }

  public PublicKey(String data) {
    super(data);
    verifier = new Ed25519Verify(this.data);
  }

  public boolean verify(Signature signature, Base64URLData data) {
    try {
      verifier.verify(signature.getData(), data.getData());
      return true;
    } catch (GeneralSecurityException e) {
      Log.d(TAG, "failed to verify witness signature " + e.getMessage());
      return false;
    }
  }
}
