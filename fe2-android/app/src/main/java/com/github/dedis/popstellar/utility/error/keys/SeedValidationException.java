package com.github.dedis.popstellar.utility.error.keys;

import com.github.dedis.popstellar.R;

public class SeedValidationException extends KeyException {

  public SeedValidationException(Exception cause) {
    super("Unable to validate given seed", cause);
  }

  @Override
  public int getUserMessage() {
    return R.string.seed_validation_exception;
  }

  @Override
  public Object[] getUserMessageArguments() {
    return new Object[0];
  }
}
