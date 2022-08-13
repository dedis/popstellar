package com.github.dedis.popstellar.model.objects.security;

import com.github.dedis.popstellar.model.Immutable;

/**
 * Represents the signature of some date.
 *
 * <p>It provides authenticity and integrity of the signed data
 */
@Immutable
public class Signature extends Base64URLData {

  public Signature(byte[] data) {
    super(data);
  }

  public Signature(String data) {
    super(data);
  }
}
