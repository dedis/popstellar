package com.github.dedis.popstellar.utility.handler;

import android.util.Log;
import com.github.dedis.popstellar.model.data.LAORepository;
import com.github.dedis.popstellar.model.data.LAOState;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.StateLao;
import com.github.dedis.popstellar.model.network.method.message.data.message.WitnessMessageSignature;

/**
 * General message handler class
 */
public class MessageHandler {

  public static final String TAG = MessageHandler.class.getSimpleName();

  private MessageHandler() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Send messages to the corresponding handler.
   *
   * @param laoRepository the repository to access the messages and LAOs
   * @param channel       the channel on which the message was received
   * @param message       the message that was received
   * @return true if the message cannot be processed and false otherwise
   */
  public static boolean handleMessage(LAORepository laoRepository, String channel,
      MessageGeneral message) {
    Log.d(TAG, "handle incoming message");

    // Put the message in the state
    laoRepository.getMessageById().put(message.getMessageId(), message);

    String senderPk = message.getSender();

    Data data = message.getData();
    Log.d(TAG, "data with class: " + data.getClass());
    boolean enqueue = false;
    if (data.getObject().equals(Objects.LAO.getObject())) {
      enqueue = LaoHandler.handleLaoMessage(laoRepository, channel, data, message.getMessageId());
    } else if (data.getObject().equals(Objects.ROLL_CALL.getObject())) {
      enqueue = RollCallHandler
          .handleRollCallMessage(laoRepository, channel, data, message.getMessageId());
    } else if (data.getObject().equals(Objects.ELECTION.getObject())) {
      enqueue = ElectionHandler
          .handleElectionMessage(laoRepository, channel, data, message.getMessageId(), senderPk);
    } else if (data.getObject().equals(Objects.MESSAGE.getObject())) {
      enqueue = WitnessMessageHandler
          .handleWitnessMessage(laoRepository, channel, senderPk, (WitnessMessageSignature) data);
    } else {
      Log.d(TAG, "cannot handle message with data" + data.getClass());
      enqueue = true;
    }

    // Trigger an onNext
    if (!(data instanceof WitnessMessageSignature) && laoRepository.isLaoChannel(channel)) {
      LAOState laoState = laoRepository.getLaoById().get(channel);
      laoState.publish();
      if (data instanceof StateLao || data instanceof CreateLao) {
        laoRepository.setAllLaoSubject();
      }
    }
    return enqueue;
  }
}
