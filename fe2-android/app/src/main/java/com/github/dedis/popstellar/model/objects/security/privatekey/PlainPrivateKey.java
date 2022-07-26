package com.github.dedis.popstellar.model.objects.security.privatekey;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.objects.security.*;
import com.google.crypto.tink.PublicKeySign;
import com.google.crypto.tink.subtle.Ed25519Sign;

import java.security.GeneralSecurityException;

/**
 * A private key where we have direct access to the key itself.
 *
 * <p>This is used by {@link com.github.dedis.popstellar.model.objects.security.PoPToken} as the key
 * pair is generated independently from the phone.
 */
public class PlainPrivateKey extends Base64URLData implements PrivateKey {

  private final PublicKeySign signer;

  public PlainPrivateKey(byte[] key) {
    super(key);

    try {
      this.signer = new Ed25519Sign(key);
    } catch (GeneralSecurityException e) {
      throw new IllegalArgumentException("Could not create the private key from its value", e);
    }
  }

  @Override
  public Signature sign(Base64URLData data) throws GeneralSecurityException {
    return new Signature(signer.sign(data.getData()));
  }

  @NonNull
  @Override
  public String toString() {
    // The actual private key should never be printed
    // Prevent that by redefining the toString representation
    return "PlainPrivateKey@" + Integer.toHexString(hashCode());
  }
}
