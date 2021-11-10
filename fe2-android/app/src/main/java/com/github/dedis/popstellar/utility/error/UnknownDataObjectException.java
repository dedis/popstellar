package com.github.dedis.popstellar.utility.error;

import com.github.dedis.popstellar.model.network.method.message.data.Data;

public class UnknownDataObjectException extends DataHandlingException {
  public UnknownDataObjectException(Data data) {
    super(data);
  }
}
