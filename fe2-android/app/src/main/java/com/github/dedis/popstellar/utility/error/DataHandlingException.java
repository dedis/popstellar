package com.github.dedis.popstellar.utility.error;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.network.method.message.data.Data;

public class DataHandlingException extends GenericException {

  @NonNull private final transient Data data;

  public DataHandlingException(@NonNull Data data, String message) {
    super(message);

    this.data = data;
  }

  public DataHandlingException(@NonNull Data data, String message, Throwable cause) {
    super(message, cause);

    this.data = data;
  }

  @NonNull
  public Data getData() {
    return data;
  }

  @Nullable
  @Override
  public String getMessage() {
    return "Error while handling data : " + super.getMessage() + "\ndata=" + data;
  }

  @Override
  public int getUserMessage() {
    return R.string.data_handling_exception;
  }

  @Override
  public Object[] getUserMessageArguments() {
    return new Object[] {data.getClass().getSimpleName()};
  }
}
