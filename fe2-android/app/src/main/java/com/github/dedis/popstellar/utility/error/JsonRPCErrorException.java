package com.github.dedis.popstellar.utility.error;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.network.answer.Error;

/** This exception is thrown when an {@link Error} response is received from a server */
public class JsonRPCErrorException extends GenericException {

  private final int errorCode;
  private final String errorDesc;

  public JsonRPCErrorException(Error error) {
    super("Error " + error.error.code + " - " + error.error.description);
    this.errorCode = error.error.code;
    this.errorDesc = error.error.description;
  }

  @Override
  public int getUserMessage() {
    return R.string.json_rpc_exception;
  }

  @Override
  public Object[] getUserMessageArguments() {
    return new Object[] {errorCode, errorDesc};
  }
}
