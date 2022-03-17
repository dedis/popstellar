package com.github.dedis.popstellar.utility.error.keys;

import com.github.dedis.popstellar.utility.error.GenericException;

/** This class regroup all the exceptions that can be generated when retrieving a key */
public abstract class KeyException extends GenericException {

  protected KeyException(String msg, Exception cause) {
    super(msg, cause);
  }

  protected KeyException(String msg) {
    super(msg);
  }
}
