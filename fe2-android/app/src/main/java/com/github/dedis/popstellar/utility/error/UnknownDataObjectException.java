package com.github.dedis.popstellar.utility.error;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.network.method.message.data.Data;

public class UnknownDataObjectException extends DataHandlingException {
  public UnknownDataObjectException(@NonNull Data data) {
    super(data, "The object " + data.getObject() + " is unknown");
  }
}
