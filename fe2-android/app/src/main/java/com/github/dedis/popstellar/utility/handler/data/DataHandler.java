package com.github.dedis.popstellar.utility.handler.data;

import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;

/**
 * Interface of functions used to handle data message. The generic type T of data need to be a
 * subclass of Data.
 */
@FunctionalInterface
public interface DataHandler<T extends Data> {

  /**
   * @param context the HandlerContext of the message
   * @param data the Data to be handle
   * @throws DataHandlingException if an error occurs
   */
  void accept(HandlerContext context, T data)
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          NoRollCallException;
}
