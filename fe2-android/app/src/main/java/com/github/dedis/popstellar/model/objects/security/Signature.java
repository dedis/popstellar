package com.github.dedis.popstellar.model.objects.security;

/**
 * Represents the signature of some date.
 *
 * <p>It provides authenticity and integrity of the signed data
 */
public class Signature extends Base64URLData {

  public Signature(byte[] data) {
    super(data);
  }

  public Signature(String data) {
    super(data);
  }

  public Signature(Signature signature) {
    this(signature.data); // Deep copy of byte array is done in parent constructor
  }
}
