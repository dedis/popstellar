package com.github.dedis.popstellar.utility.error;

import com.github.dedis.popstellar.model.network.answer.Error;

public class JsonRPCErrorException extends Exception {
  public JsonRPCErrorException(Error error) {
    super("Error " + error.getError().getCode() + " : " + error.getError().getDescription());
  }
}
