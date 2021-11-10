package com.github.dedis.popstellar.utility.error;

import com.github.dedis.popstellar.model.network.method.message.data.Data;

public class InvalidMessageIdException extends DataHandlingException {

  public InvalidMessageIdException(Data data) {
    super(data);
  }
}
