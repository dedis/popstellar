package com.github.dedis.popstellar;

public class PoPException extends RuntimeException {

  public PoPException(String message) {
    super(message);
  }

  public PoPException(String message, Throwable cause) {
    super(message, cause);
  }
}
