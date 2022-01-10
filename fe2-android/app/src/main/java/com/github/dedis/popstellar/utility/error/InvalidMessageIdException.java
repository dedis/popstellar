package com.github.dedis.popstellar.utility.error;

import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.objects.security.MessageID;

public class InvalidMessageIdException extends InvalidDataException {

  public InvalidMessageIdException(Data data, String id) {
    super(data, "message id", id);
  }

  public InvalidMessageIdException(Data data, MessageID id) {
    super(data, "message id", id.getEncoded());
  }
}
