package com.github.dedis.popstellar.utility.error;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.network.method.message.data.Data;

public class UnknownDataActionException extends DataHandlingException {

  public UnknownDataActionException(@NonNull Data data) {
    super(data, "The action " + data.getAction() + " is unknown");
  }
}
