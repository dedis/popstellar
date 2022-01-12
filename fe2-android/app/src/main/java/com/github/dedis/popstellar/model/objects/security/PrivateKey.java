package com.github.dedis.popstellar.model.objects.security;

import java.security.GeneralSecurityException;

/** A private key that can be used to sign data */
public interface PrivateKey {

  /**
   * Signs some data and returns the generated {@link Signature}
   *
   * @param data to sign
   * @return generated signature
   * @throws GeneralSecurityException if an error occurs while signing
   */
  Signature sign(Base64URLData data) throws GeneralSecurityException;
}
