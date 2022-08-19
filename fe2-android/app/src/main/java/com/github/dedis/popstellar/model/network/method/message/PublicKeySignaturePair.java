package com.github.dedis.popstellar.model.network.method.message;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.security.Signature;

@Immutable
public class PublicKeySignaturePair {

  private final PublicKey witness;
  private final Signature signature;

  public PublicKeySignaturePair(PublicKey witness, Signature signature) {
    this.witness = witness;
    this.signature = signature;
  }

  public PublicKey getWitness() {
    return witness;
  }

  public Signature getSignature() {
    return signature;
  }

  @NonNull
  @Override
  public String toString() {
    return "PublicKeySignaturePair{"
        + "witness="
        + witness.getEncoded()
        + ", signature="
        + signature.getEncoded()
        + '}';
  }
}
