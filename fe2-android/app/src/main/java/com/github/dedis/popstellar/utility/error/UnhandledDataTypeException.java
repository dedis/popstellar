package com.github.dedis.popstellar.utility.error;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.network.method.message.data.Data;

public class UnhandledDataTypeException extends DataHandlingException {

  public UnhandledDataTypeException(@NonNull Data data, String type) {
    super(
        data,
        String.format(
            "The pair (%s, %s) is not handled by the system because of %s",
            data.getObject(), data.getAction(), type));
  }
}
