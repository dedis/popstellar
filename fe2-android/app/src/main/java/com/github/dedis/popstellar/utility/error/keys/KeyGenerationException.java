package com.github.dedis.popstellar.utility.error.keys;

import java.security.GeneralSecurityException;

public class KeyGenerationException extends KeyException {

  public KeyGenerationException(GeneralSecurityException e) {
    super("Could not generate key", e);
  }
}
