package com.github.dedis.popstellar.model.network.method.message;

import java.util.Base64;

public class PublicKeySignaturePair {

  private final String witness;

  private final String signature;

  public PublicKeySignaturePair(byte[] witness, byte[] signature) {
    this.witness = Base64.getUrlEncoder().encodeToString(witness);
    this.signature = Base64.getUrlEncoder().encodeToString(signature);
  }

  public byte[] getWitness() {
    return Base64.getUrlDecoder().decode(witness);
  }

  public byte[] getSignature() {
    return Base64.getUrlDecoder().decode(signature);
  }

  public String getWitnessEncoded() {
    return witness;
  }

  public String getSignatureEncoded() {
    return signature;
  }

  @Override
  public String toString() {
    return "PublicKeySignaturePair{"
        + "witness="
        + getWitnessEncoded()
        + ", signature="
        + getSignatureEncoded()
        + '}';
  }
}
