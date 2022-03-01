package com.github.dedis.popstellar.utility.error.keys;

import com.github.dedis.popstellar.R;

import java.security.GeneralSecurityException;

public class KeyGenerationException extends KeyException {

  public KeyGenerationException(GeneralSecurityException e) {
    super("Could not generate key", e);
  }

  @Override
  public int getUserMessage() {
    return R.string.key_generation_exception;
  }

  @Override
  public Object[] getUserMessageArguments() {
    return new Object[0];
  }
}
