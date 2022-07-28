package com.github.dedis.popstellar.model.objects.security.privatekey;

import com.github.dedis.popstellar.model.objects.security.*;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.PublicKeySign;

import java.security.GeneralSecurityException;

/**
 * A private key where the key is protected by android and thus inaccessible.
 *
 * <p>We do not have access to the key, but we have access to the primitives derived from it like a
 * {@link PublicKeySign} that can be used to sign data.
 */
public class ProtectedPrivateKey implements PrivateKey {

  private final PublicKeySign signer;

  public ProtectedPrivateKey(KeysetHandle handler) {
    try {
      this.signer = handler.getPrimitive(PublicKeySign.class);
    } catch (GeneralSecurityException e) {
      throw new IllegalArgumentException("Could not create the private key from the keyset", e);
    }
  }

  @Override
  public Signature sign(Base64URLData data) throws GeneralSecurityException {
    return new Signature(signer.sign(data.getData()));
  }
}
