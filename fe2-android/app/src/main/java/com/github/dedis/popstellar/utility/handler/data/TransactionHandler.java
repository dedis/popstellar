package com.github.dedis.popstellar.utility.handler.data;

import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.PostTransaction;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.Transaction;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.repository.LAORepository;

/** Transaction messages handler class */
public class TransactionHandler {
  public static final String TAG = TransactionHandler.class.getSimpleName();

  private TransactionHandler() {
    throw new IllegalArgumentException("Utility class");
  }

  /**
   * Process an postTransaction
   *
   * @param context the HandlerContext of the message
   * @param postTransaction the data of the message received
   */
  public static void handlePostTransaction(
      HandlerContext context, PostTransaction postTransaction) {
    LAORepository laoRepository = context.getLaoRepository();
    Channel channel = context.getChannel();
    Lao lao = laoRepository.getLaoByChannel(channel);
    Transaction transaction =
        new Transaction(
            postTransaction.getTransaction().getVersion(),
            postTransaction.getTransaction().getTxIns(),
            postTransaction.getTransaction().getTxOuts(),
            postTransaction.getTransaction().getTimestamp());

    lao.updateTransaction(transaction);
  }
}
