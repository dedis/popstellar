package com.github.dedis.popstellar.utility.error;

import com.github.dedis.popstellar.model.network.method.message.data.Data;

public class InvalidSignatureException extends InvalidDataException {

  public InvalidSignatureException(Data data, String signature) {
    super(data, "signature", signature);
  }
}
