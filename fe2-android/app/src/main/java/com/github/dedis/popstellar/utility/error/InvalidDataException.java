package com.github.dedis.popstellar.utility.error;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.network.method.message.data.Data;

public class InvalidDataException extends DataHandlingException {

  public InvalidDataException(@NonNull Data data, String dataType, String dataValue) {
    super(data, "Invalid " + dataType + " " + dataValue);
  }
}
