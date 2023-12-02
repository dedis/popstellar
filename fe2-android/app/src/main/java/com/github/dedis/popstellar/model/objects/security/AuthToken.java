package com.github.dedis.popstellar.model.objects.security;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.objects.security.privatekey.PlainPrivateKey;

/** Represents a AuthToken key pair with its private and public keys */
@Immutable
public class AuthToken extends KeyPair {

  public AuthToken(byte[] privateKey, byte[] publicKey) {
    super(new PlainPrivateKey(privateKey), new PublicKey(publicKey));
  }

  public AuthToken(PoPToken otherToken) {
    super(otherToken.getPrivateKey(), otherToken.getPublicKey());
  }

  @Override
  public PlainPrivateKey getPrivateKey() {
    return (PlainPrivateKey) super.getPrivateKey();
  }
}
