package com.github.dedis.student20_pop.model.network.method.message;

public class PublicKeySignaturePair {

  private byte[] witness;

  private byte[] signature;

  public PublicKeySignaturePair(byte[] witness, byte[] signature) {
    this.witness = witness;
    this.signature = signature;
  }
}
