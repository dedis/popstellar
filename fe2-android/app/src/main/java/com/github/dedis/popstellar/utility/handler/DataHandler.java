package com.github.dedis.popstellar.utility.handler;

import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.utility.error.DataHandlingException;

@FunctionalInterface
public interface DataHandler<T> {

  void accept(HandlerContext context, T data) throws DataHandlingException;

  @SuppressWarnings("unchecked")
  default void accept(HandlerContext context, Data data)
      throws DataHandlingException, ClassCastException {
    accept(context, (T) data);
  }
}
