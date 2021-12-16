package com.github.dedis.popstellar.model.objects.security;

import com.google.crypto.tink.PublicKeySign;

import java.security.GeneralSecurityException;

/**
 * Represent a private/public key pair used for signing data
 *
 * <p>This object does not actually store the private key as is it safely stored by another module.
 */
public class KeyPair {

  /**
   * A key pair does not always have access to the actual private key. But it has access to the
   * functionalities like the signing.
   */
  private final PublicKeySign signer;

  private final PublicKey publicKey;

  public KeyPair(PublicKeySign signer, PublicKey publicKey) {
    this.signer = signer;
    this.publicKey = publicKey;
  }

  /**
   * Signs the given data with the private key of the pair {@link PublicKeySign#sign(byte[])}
   *
   * @param data to sign
   * @return the signature
   * @throws GeneralSecurityException if an error occurs
   */
  public byte[] sign(byte[] data) throws GeneralSecurityException {
    return signer.sign(data);
  }

  public PublicKey getPublicKey() {
    return publicKey;
  }
}
