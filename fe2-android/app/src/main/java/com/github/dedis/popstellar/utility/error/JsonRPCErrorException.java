package com.github.dedis.popstellar.utility.error;

import com.github.dedis.popstellar.model.network.answer.Error;

/** This exception is thrown when an {@link Error} response is received from a server */
public class JsonRPCErrorException extends Exception {
  public JsonRPCErrorException(Error error) {
    super("Error " + error.getError().getCode() + " : " + error.getError().getDescription());
  }
}
