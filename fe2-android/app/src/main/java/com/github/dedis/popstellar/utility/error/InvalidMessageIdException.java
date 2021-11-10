package com.github.dedis.popstellar.utility.error;

import com.github.dedis.popstellar.model.network.method.message.data.Data;

public class InvalidMessageIdException extends InvalidDataException {

  public InvalidMessageIdException(Data data, String id) {
    super(data, "message id", id);
  }
}
