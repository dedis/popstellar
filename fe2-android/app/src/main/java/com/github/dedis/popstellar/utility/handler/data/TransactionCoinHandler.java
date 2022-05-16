package com.github.dedis.popstellar.utility.handler.data;

import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.PostTransactionCoin;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.repository.LAORepository;

public class TransactionCoinHandler {
  public static final String TAG = TransactionCoinHandler.class.getSimpleName();

  private TransactionCoinHandler() {
    throw new IllegalArgumentException("Utility class");
  }

  /**
   * Process an PostTransactionCoin.
   *
   * @param context the HandlerContext of the message
   * @param postTransactionCoin the data of the message that was received
   */
  public static void handlePostTransactionCoin(
      HandlerContext context, PostTransactionCoin postTransactionCoin) {
    LAORepository laoRepository = context.getLaoRepository();
    Channel channel = context.getChannel();
    Lao lao = laoRepository.getLaoByChannel(channel);

    // update all transaction

    // lao update the history

    // lao update the last transaction per public key
  }
}
