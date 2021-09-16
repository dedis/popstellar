package com.github.dedis.popstellar.exceptions;

public class PoPException extends RuntimeException {

  public PoPException(String message) {
    super(message);
  }

  public PoPException(String message, Throwable cause) {
    super(message, cause);
  }
}
