package com.github.dedis.popstellar.utility.error;

import com.github.dedis.popstellar.model.network.method.message.data.Data;

/** This exception is raised when a message is handled at the wrong state of an object */
public class InvalidStateException extends DataHandlingException {

  public InvalidStateException(Data data, String object, String state, String expected) {
    super(
        data,
        "The " + object + " is in the wrong state. It is in " + state + " expected " + expected);
  }
}
