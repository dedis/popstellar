package com.github.dedis.popstellar.model.network.method.message;

import java.util.Base64;

public class PublicKeySignaturePair {

  private byte[] witness;

  private byte[] signature;

  public PublicKeySignaturePair(byte[] witness, byte[] signature) {
    this.witness = witness;
    this.signature = signature;
  }

  public byte[] getWitness() {
    return witness;
  }

  public byte[] getSignature() {
    return signature;
  }

  public String getWitnessEncoded() {
    return Base64.getUrlEncoder().encodeToString(this.witness);
  }

  public String getSignatureEncoded() {
    return Base64.getUrlEncoder().encodeToString(this.signature);
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
