package com.github.dedis.popstellar.model.objects.security;

import com.github.dedis.popstellar.model.objects.security.privatekey.PlainPrivateKey;

/** Represents a PoPToken key pair with its private and public keys */
public class PoPToken extends KeyPair {

  public PoPToken(byte[] privateKey, byte[] publicKey) {
    super(new PlainPrivateKey(privateKey), new PublicKey(publicKey));
  }
}
