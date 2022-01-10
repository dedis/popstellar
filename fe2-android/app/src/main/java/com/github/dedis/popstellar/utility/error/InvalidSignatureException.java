package com.github.dedis.popstellar.utility.error;

import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.objects.security.Signature;

public class InvalidSignatureException extends InvalidDataException {

  public InvalidSignatureException(Data data, Signature signature) {
    super(data, "signature", signature.getEncoded());
  }
}
