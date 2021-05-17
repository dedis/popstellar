package com.github.dedis.student20_pop.model.network.method.message;

import android.util.Base64;

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
    return Base64.encodeToString(this.witness, Base64.NO_WRAP);
  }

  public String getSignatureEncoded() {
    return Base64.encodeToString(this.signature, Base64.NO_WRAP);
  }
}
