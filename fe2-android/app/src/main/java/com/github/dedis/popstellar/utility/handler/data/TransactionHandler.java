package com.github.dedis.popstellar.utility.handler.data;

import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.PostTransaction;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.LAORepository;

public class TransactionHandler {

  public static final String TAG = TransactionHandler.class.getSimpleName();

  private TransactionHandler() {
    throw new IllegalArgumentException("Utility class");
  }

  /**
   * Process an PostTransaction message.
   *
   * @param context the HandlerContext of the message
   * @param postTransaction the data of the message that was received
   */
  public static void handlePostTransaction(
      HandlerContext context, PostTransaction postTransaction) {
    LAORepository laoRepository = context.getLaoRepository();
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();
    PublicKey senderPk = context.getSenderPk();

    Lao lao = laoRepository.getLaoByChannel(channel);
  }
}
