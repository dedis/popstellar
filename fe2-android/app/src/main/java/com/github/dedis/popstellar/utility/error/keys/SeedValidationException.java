package com.github.dedis.popstellar.utility.error.keys;

public class SeedValidationException extends KeyException {

  public SeedValidationException(Exception cause) {
    super("Unable to validate given seed", cause);
  }
}
