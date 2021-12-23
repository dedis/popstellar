package com.github.dedis.popstellar.utility.handler;

import com.github.dedis.popstellar.utility.error.DataHandlingException;

@FunctionalInterface
public interface DataHandler<T> {

  void accept(HandlerContext context, T data) throws DataHandlingException;
}
