package com.github.dedis.popstellar.utility.error;

import androidx.annotation.StringRes;

public abstract class GenericException extends Exception {

  protected GenericException(String message) {
    super(message);
  }

  protected GenericException(String message, Throwable cause) {
    super(message, cause);
  }

  protected GenericException() {}

  @StringRes
  public abstract int getUserMessage();

  public abstract Object[] getUserMessageArguments();
}
