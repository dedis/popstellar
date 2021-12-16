package com.github.dedis.popstellar.model.objects.security;

import com.google.crypto.tink.PublicKeySign;
import com.google.crypto.tink.subtle.Ed25519Sign;

import java.security.GeneralSecurityException;

/**
 * A private key that can be used to sign data
 *
 * <p>It should never be logged or sent publicly
 */
public class PrivateKey extends Base64URLData implements PublicKeySign {

  private final PublicKeySign signer;

  public PrivateKey(byte[] data) throws GeneralSecurityException {
    super(data);
    signer = new Ed25519Sign(data);
  }

  public PrivateKey(String data) throws GeneralSecurityException {
    super(data);
    signer = new Ed25519Sign(this.data);
  }

  public Signature sign(Base64URLData data) throws GeneralSecurityException {
    return new Signature(sign(data.getData()));
  }

  @Override
  public byte[] sign(byte[] data) throws GeneralSecurityException {
    return signer.sign(data);
  }
}
