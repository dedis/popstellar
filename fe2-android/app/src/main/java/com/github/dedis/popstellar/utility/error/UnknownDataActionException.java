package com.github.dedis.popstellar.utility.error;

import com.github.dedis.popstellar.model.network.method.message.data.Data;

public class UnknownDataActionException extends DataHandlingException {
  public UnknownDataActionException(Data data) {
    super(data);
  }
}
