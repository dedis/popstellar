package com.github.dedis.popstellar.utility.handler;

import static com.github.dedis.popstellar.utility.handler.MessageHandler.handleMessage;

import android.util.Log;

import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Error;
import com.github.dedis.popstellar.model.network.answer.Result;
import com.github.dedis.popstellar.model.network.answer.ResultMessages;
import com.github.dedis.popstellar.model.network.method.Broadcast;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.LAOState;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.reactivex.subjects.Subject;

/** Subscribe, catchup, create LAO and broadcast handler class */
public class GenericHandler {

  public static final String TAG = GenericHandler.class.getSimpleName();

  private GenericHandler() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Delete the pending requests with the id of the error
   *
   * @param genericMessage the message received
   * @param subscribeRequests the pending subscribe requests
   * @param catchupRequests the pending catchup requests
   * @param createLaoRequests the pending create lao requests
   */
  public static void handleError(
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
  public static void handleSubscribe(
      LAORepository laoRepository, int id, Map<Integer, String> subscribeRequests) {
    String channel = subscribeRequests.get(id);
    subscribeRequests.remove(id);

    if (laoRepository.isLaoChannel(channel)) {
      Log.d(TAG, "subscribing to LAO with id " + channel);

      // Create the new LAO and add it to the LAORepository LAO lists
      Lao lao = new Lao(channel);
      laoRepository.getLaoById().put(channel, new LAOState(lao));
      laoRepository.setAllLaoSubject();

      // Send catchup after subscribing to a LAO
      laoRepository.sendCatchup(channel);
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
  public static void handleCatchup(
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

    Log.d(TAG, "messages length: " + messages.size());
    // Handle all received messages from the catchup
    for (MessageGeneral msg : messages) {
      boolean enqueue = handleMessage(laoRepository, channel, msg);
      if (enqueue) {
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
  public static void handleCreateLao(
      LAORepository laoRepository, int id, Map<Integer, String> createLaoRequests) {
    Log.d(TAG, "createLaoRequest contains this id");
    String channel = createLaoRequests.get(id);
    createLaoRequests.remove(id);

    // Create new LAO and add it to the LAORepository LAO lists
    Lao lao = new Lao(channel);
    laoRepository.getLaoById().put(channel, new LAOState(lao));
    laoRepository.setAllLaoSubject();

    // Send subscribe and catchup after creating a LAO
    laoRepository.sendSubscribe(channel);
    laoRepository.sendCatchup(channel);
  }

  /**
   * Send the broadcast messages to the message handler
   *
   * @param genericMessage the generic message received
   * @param unprocessed the unprocessed messages
   */
  public static void handleBroadcast(
      LAORepository laoRepository,
      GenericMessage genericMessage,
      Subject<GenericMessage> unprocessed) {
    Broadcast broadcast = (Broadcast) genericMessage;
    MessageGeneral message = broadcast.getMessage();
    String channel = broadcast.getChannel();

    Log.d(TAG, "broadcast channel: " + channel + " message " + message.getMessageId());

    boolean enqueue = handleMessage(laoRepository, channel, message);
    if (enqueue) {
      unprocessed.onNext(genericMessage);
    }
  }
}
