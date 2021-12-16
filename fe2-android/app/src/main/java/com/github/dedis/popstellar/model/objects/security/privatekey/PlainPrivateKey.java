package com.github.dedis.popstellar.model.objects.security.privatekey;

import com.github.dedis.popstellar.model.objects.security.Base64URLData;
import com.github.dedis.popstellar.model.objects.security.PrivateKey;
import com.github.dedis.popstellar.model.objects.security.Signature;
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
}
