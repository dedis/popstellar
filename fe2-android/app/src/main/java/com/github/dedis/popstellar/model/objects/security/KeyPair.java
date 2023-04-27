package com.github.dedis.popstellar.model.objects.security;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.Immutable;
import com.google.crypto.tink.PublicKeySign;

import java.security.GeneralSecurityException;
import java.util.Objects;

/**
 * Represent a private/public key pair used for signing data
 *
 * <p>This object does not actually store the private key as is it safely stored by another module.
 */
@Immutable
public class KeyPair {

  private final PrivateKey privateKey;
  private final PublicKey publicKey;

  public KeyPair(PrivateKey privateKey, PublicKey publicKey) {
    this.privateKey = privateKey;
    this.publicKey = publicKey;
  }

  /**
   * Signs the given data with the private key of the pair {@link PublicKeySign#sign(byte[])}
   *
   * @param data to sign
   * @return the signature
   * @throws GeneralSecurityException if an error occurs
   */
  public Signature sign(Base64URLData data) throws GeneralSecurityException {
    return privateKey.sign(data);
  }

  public boolean verify(Signature signature, Base64URLData data) {
    return publicKey.verify(signature, data);
  }

  public PrivateKey getPrivateKey() {
    return privateKey;
  }

  public PublicKey getPublicKey() {
    return publicKey;
  }

  @NonNull
  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{privateKey="
        + privateKey.toString()
        + ", publicKey="
        + publicKey.toString()
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    KeyPair keyPair = (KeyPair) o;
    return Objects.equals(privateKey, keyPair.privateKey)
        && Objects.equals(publicKey, keyPair.publicKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(privateKey, publicKey);
  }
}
