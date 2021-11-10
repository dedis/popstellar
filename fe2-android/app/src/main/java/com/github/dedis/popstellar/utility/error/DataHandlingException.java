package com.github.dedis.popstellar.utility.error;

import androidx.annotation.Nullable;

import com.github.dedis.popstellar.model.network.method.message.data.Data;

public class DataHandlingException extends Exception {

  @Nullable private final Data data;

  public DataHandlingException(@Nullable Data data) {
    this.data = data;
  }

  public DataHandlingException(@Nullable Data data, String message) {
    super(message);

    this.data = data;
  }

  public DataHandlingException(@Nullable Data data, String message, Throwable cause) {
    super(message, cause);

    this.data = data;
  }

  public DataHandlingException(@Nullable Data data, Throwable cause) {
    super(cause);

    this.data = data;
  }

  @Nullable
  public Data getData() {
    return data;
  }
}
