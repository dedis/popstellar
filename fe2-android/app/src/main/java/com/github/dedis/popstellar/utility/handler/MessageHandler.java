package com.github.dedis.popstellar.utility.handler;

import android.util.Log;

import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.DataRegistry;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.StateLao;
import com.github.dedis.popstellar.model.network.method.message.data.message.WitnessMessageSignature;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.LAOState;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.utility.error.DataHandlingException;
import com.github.dedis.popstellar.utility.handler.data.HandlerContext;
import com.github.dedis.popstellar.utility.security.KeyManager;

import javax.inject.Inject;
import javax.inject.Singleton;

/** General message handler class */
@Singleton
public final class MessageHandler {

  public static final String TAG = MessageHandler.class.getSimpleName();

  private final DataRegistry registry;
  private final KeyManager keyManager;

  @Inject
  public MessageHandler(DataRegistry registry, KeyManager keyManager) {
    this.registry = registry;
    this.keyManager = keyManager;
  }

  /**
   * Send messages to the corresponding handler.
   *
   * @param laoRepository the repository to access the messages and LAOs
   * @param messageSender the service used to send messages to the backend
   * @param channel the channel on which the message was received
   * @param message the message that was received
   */
  public void handleMessage(
      LAORepository laoRepository,
      MessageSender messageSender,
      String channel,
      MessageGeneral message)
      throws DataHandlingException {
    Log.d(TAG, "handle incoming message");

    // Put the message in the state
    laoRepository.getMessageById().put(message.getMessageId(), message);

    Data data = message.getData();
    Log.d(TAG, "data with class: " + data.getClass());

    Objects dataObj = Objects.find(data.getObject());
    Action dataAction = Action.find(data.getAction());

    registry.handle(
        new HandlerContext(laoRepository, keyManager, messageSender, channel, message),
        data,
        dataObj,
        dataAction);

    notifyLaoUpdate(laoRepository, data, channel);
  }

  /**
   * Keep the UI up to date by notifying all observers the updated LAO state.
   *
   * <p>The LAO is updated if the channel of the message is a LAO channel and the message is not a
   * WitnessSignatureMessage.
   *
   * <p>If a LAO has been created or modified then the LAO lists in the LAORepository are updated.
   *
   * @param laoRepository the repository to access the LAO lists
   * @param data the data received
   * @param channel the channel of the message received
   */
  private void notifyLaoUpdate(LAORepository laoRepository, Data data, String channel) {
    if (!(data instanceof WitnessMessageSignature) && laoRepository.isLaoChannel(channel)) {
      LAOState laoState = laoRepository.getLaoByChannel().get(channel);
      laoState.publish(); // Trigger an onNext
      if (data instanceof StateLao || data instanceof CreateLao) {
        laoRepository.setAllLaoSubject();
      }
    }
  }
}
