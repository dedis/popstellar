package com.github.dedis.popstellar.utility.handler;

import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.utility.error.DataHandlingException;

@FunctionalInterface
public interface DataConsumer<T> {

  void accept(
      LAORepository laoRepository, String channel, T data, String messageId, String senderPk)
      throws DataHandlingException;

  @SuppressWarnings("unchecked")
  default void accept(
      LAORepository laoRepository, String channel, Data data, String messageId, String senderPk)
      throws DataHandlingException, ClassCastException {
    accept(laoRepository, channel, (T) data, messageId, senderPk);
  }
}
