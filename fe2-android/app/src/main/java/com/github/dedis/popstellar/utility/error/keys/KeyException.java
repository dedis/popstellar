package com.github.dedis.popstellar.utility.error.keys;

/** This class regroup all the exceptions that can be generated when retrieving a key */
public abstract class KeyException extends Exception {

  protected KeyException(String msg, Exception cause) {
    super(msg, cause);
  }

  protected KeyException(String msg) {
    super(msg);
  }
}
