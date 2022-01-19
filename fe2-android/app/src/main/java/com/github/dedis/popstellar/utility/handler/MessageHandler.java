package com.github.dedis.popstellar.utility.handler;

import android.util.Log;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Error;
import com.github.dedis.popstellar.model.network.answer.Result;
import com.github.dedis.popstellar.model.network.answer.ResultMessages;
import com.github.dedis.popstellar.model.network.method.Broadcast;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.DataRegistry;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.network.method.message.data.lao.StateLao;
import com.github.dedis.popstellar.model.network.method.message.data.message.WitnessMessageSignature;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.LAOState;
import com.github.dedis.popstellar.utility.error.DataHandlingException;
import com.github.dedis.popstellar.utility.handler.data.HandlerContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.subjects.Subject;

/** General message handler class */
@Singleton
public final class MessageHandler {

  public static final String TAG = MessageHandler.class.getSimpleName();

  private final DataRegistry registry;

  @Inject
  public MessageHandler(DataRegistry registry) {
    this.registry = registry;
  }

  /**
   * Delete the pending requests with the id of the error
   *
   * @param genericMessage the message received
   * @param subscribeRequests the pending subscribe requests
   * @param catchupRequests the pending catchup requests
   * @param createLaoRequests the pending create lao requests
   */
  public void handleError(
      GenericMessage genericMessage,
      Map<Integer, String> subscribeRequests,
      Map<Integer, String> catchupRequests,
      Map<Integer, String> createLaoRequests) {
    Error err = (Error) genericMessage;
    int id = err.getId();
    Log.d(TAG, "got an error answer with id " + id);

    subscribeRequests.remove(id);
    catchupRequests.remove(id);
    createLaoRequests.remove(id);
  }

  /**
   * Handle a subscribe request. When subscribing to a LAO create the LAO and send a catchup
   *
   * @param laoRepository the repository to access the LAOs
   * @param id the id of the subscribe request
   * @param subscribeRequests the pending subscribe requests
   */
  public void handleSubscribe(
      LAORepository laoRepository, int id, Map<Integer, String> subscribeRequests) {
    String channel = subscribeRequests.remove(id);

    if (channel != null && laoRepository.isLaoChannel(channel)) {
      Log.d(TAG, "subscribing to LAO with id " + channel);

      // Create the new LAO and add it to the LAORepository LAO lists
      Lao lao = new Lao(channel.replace("/root/", ""));
      laoRepository.getLaoById().put(channel, new LAOState(lao));
      laoRepository.setAllLaoSubject();

      // Send catchup after subscribing to a LAO
      laoRepository.sendCatchup(channel);
    } else if (channel != null) {
      laoRepository.sendCatchup(channel);
    } else {
      Log.e(TAG, "Invalid Subscribe request id : " + id);
    }
  }

  /**
   * Handle a catchup request by handling all received messages
   *
   * @param laoRepository the repository to access the LAOs
   * @param id the id of the catchup request
   * @param result the result message received
   * @param catchupRequests the pending catchup requests
   * @param unprocessed the unprocessed messages
   */
  public void handleCatchup(
      LAORepository laoRepository,
      int id,
      Result result,
      Map<Integer, String> catchupRequests,
      Subject<GenericMessage> unprocessed) {
    Log.d(TAG, "got a catchup request in response to request id " + id);
    String channel = catchupRequests.get(id);
    catchupRequests.remove(id);

    List<MessageGeneral> messages = Collections.emptyList();
    if (result instanceof ResultMessages) {
      messages = ((ResultMessages) result).getMessages();
    } else {
      Log.w(
          TAG,
          "Invalid type of Result '"
              + result.getClass().getSimpleName()
              + "' for catchup with id : "
              + id);
    }

    Log.d(TAG, "Messages to handle : " + messages.size());
    // Handle all received messages from the catchup
    for (MessageGeneral msg : messages) {
      try {
        handleMessage(laoRepository, channel, msg);
      } catch (DataHandlingException ex) {
        Log.e(TAG, "Unable to handle message", ex);
        // This will have to change at some point. Every error is added to the queue.
        // So if a message always fails to be handled, it will be re-added in the queue forever
        unprocessed.onNext(result);
      }
    }
  }

  /**
   * Handle a create LAO request. First create the LAO then subscribe to the LAO channel and finally
   * send a catchup request
   *
   * @param laoRepository the repository to access the LAOs
   * @param id the id of the create LAO request
   * @param createLaoRequests the pending create LAO requests
   */
  public void handleCreateLao(
      LAORepository laoRepository, int id, Map<Integer, String> createLaoRequests) {
    Log.d(TAG, "createLaoRequest contains this id");
    String channel = createLaoRequests.get(id);
    createLaoRequests.remove(id);

    // Create new LAO and add it to the LAORepository LAO lists
    Lao lao = new Lao(channel.replace("/root/", ""));
    laoRepository.getLaoById().put(channel, new LAOState(lao));
    laoRepository.setAllLaoSubject();

    // Send subscribe and catchup after creating a LAO
    laoRepository.sendSubscribe(channel);
    laoRepository.sendSubscribe(channel + "/consensus");
  }

  /**
   * Send the broadcast messages to the message handler
   *
   * @param genericMessage the generic message received
   * @param unprocessed the unprocessed messages
   */
  public void handleBroadcast(
      LAORepository laoRepository,
      GenericMessage genericMessage,
      Subject<GenericMessage> unprocessed) {
    Broadcast broadcast = (Broadcast) genericMessage;
    MessageGeneral message = broadcast.getMessage();
    String channel = broadcast.getChannel();

    Log.d(TAG, "broadcast channel: " + channel + " message " + message.getMessageId());

    try {
      handleMessage(laoRepository, channel, message);
    } catch (DataHandlingException ex) {
      Log.e(TAG, "Unable to handle message", ex);
      // This will have to change at some point. Every error is added to the queue.
      // So if a message always fails to be handled, it will be re-added in the queue forever
      unprocessed.onNext(broadcast);
    }
  }

  /**
   * Send messages to the corresponding handler.
   *
   * @param laoRepository the repository to access the messages and LAOs
   * @param channel the channel on which the message was received
   * @param message the message that was received
   */
  public void handleMessage(LAORepository laoRepository, String channel, MessageGeneral message)
      throws DataHandlingException {
    Log.d(TAG, "handle incoming message");

    // Put the message in the state
    laoRepository.getMessageById().put(message.getMessageId(), message);

    Data data = message.getData();
    Log.d(TAG, "data with class: " + data.getClass());

    Objects dataObj = Objects.find(data.getObject());
    Action dataAction = Action.find(data.getAction());

    registry.handle(new HandlerContext(laoRepository, channel, message), data, dataObj, dataAction);

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
      LAOState laoState = laoRepository.getLaoById().get(channel);
      laoState.publish(); // Trigger an onNext
      if (data instanceof StateLao || data instanceof CreateLao) {
        laoRepository.setAllLaoSubject();
      }
    }
  }
}
