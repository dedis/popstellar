package com.github.dedis.popstellar.utility.error;

import androidx.annotation.StringRes;

public abstract class GenericException extends Exception {

  public GenericException(String message) {
    super(message);
  }

  public GenericException(String message, Throwable cause) {
    super(message, cause);
  }

  @StringRes
  public abstract int getUserMessage();

  public abstract Object[] getUserMessageArguments();
}
