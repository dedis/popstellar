package com.github.dedis.popstellar.utility.error.keys;

/** This class regroup all the exceptions that can be generated when retrieving a key */
public abstract class KeyException extends Exception {

  public KeyException(String msg, Exception cause) {
    super(msg, cause);
  }

  public KeyException(String msg) {
    super(msg);
  }
}
