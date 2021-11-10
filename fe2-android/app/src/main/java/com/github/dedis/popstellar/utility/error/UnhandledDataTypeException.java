package com.github.dedis.popstellar.utility.error;

import androidx.annotation.Nullable;

import com.github.dedis.popstellar.model.network.method.message.data.Data;

public class UnhandledDataTypeException extends DataHandlingException {

  public UnhandledDataTypeException(@Nullable Data data, String type) {
    super(data);
  }
}
