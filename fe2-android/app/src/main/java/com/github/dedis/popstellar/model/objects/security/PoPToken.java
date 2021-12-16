package com.github.dedis.popstellar.model.objects.security;

/** Represents a PoPToken key pair with its private and public keys */
public class PoPToken extends KeyPair {

  private final PrivateKey privateKey;

  public PoPToken(PrivateKey privateKey, PublicKey publicKey) {
    super(privateKey, publicKey);

    this.privateKey = privateKey;
  }

  public PrivateKey getPrivateKey() {
    return privateKey;
  }
}
