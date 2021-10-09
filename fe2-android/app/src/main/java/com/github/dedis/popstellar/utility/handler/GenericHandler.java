package com.github.dedis.popstellar.utility.handler;

import static com.github.dedis.popstellar.utility.handler.MessageHandler.handleMessage;

import android.util.Log;
import com.github.dedis.popstellar.model.network.GenericMessage;
import com.github.dedis.popstellar.model.network.answer.Error;
import com.github.dedis.popstellar.model.network.answer.ResultMessages;
import com.github.dedis.popstellar.model.network.method.Broadcast;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.LAOState;
import io.reactivex.subjects.Subject;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Subscribe, catchup, create LAO and broadcast handler class
 */
public class GenericHandler {

  public static final String TAG = GenericHandler.class.getSimpleName();

  private GenericHandler() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Delete the pending requests with the id of the error
   *
   * @param genericMessage    the message received
   * @param subscribeRequests the pending subscribe requests
   * @param catchupRequests   the pending catchup requests
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
    if (subscribeRequests.containsKey(id)) {
      subscribeRequests.remove(id);
    } else if (catchupRequests.containsKey(id)) {
      catchupRequests.remove(id);
    } else if (createLaoRequests.containsKey(id)) {
      createLaoRequests.remove(id);
    }
  }

  /**
   * Handle a subscribe request. When subscribing to a LAO create the LAO and send a catchup
   *
   * @param laoRepository     the repository to access the LAOs
   * @param id                the id of the subscribe request
   * @param subscribeRequests the pending subscribe requests
   */
  public static void handleSubscribe(
      LAORepository laoRepository, int id,
      Map<Integer, String> subscribeRequests) {
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
   * @param laoRepository   the repository to access the LAOs
   * @param id              the id of the catchup request
   * @param genericMessage  the generic message received
   * @param catchupRequests the pending catchup requests
   * @param unprocessed     the unprocessed messages
   */
  public static void handleCatchup(
      LAORepository laoRepository, int id,
      GenericMessage genericMessage,
      Map<Integer, String> catchupRequests,
      Subject<GenericMessage> unprocessed) {
    Log.d(TAG, "got a catchup request in response to request id " + id);
    String channel = catchupRequests.get(id);
    catchupRequests.remove(id);

    List<MessageGeneral> messages = Collections.emptyList();
    if (genericMessage instanceof ResultMessages) {
      messages = ((ResultMessages) genericMessage).getMessages();
    }

    Log.d(TAG, "messages length: " + messages.size());
    // Handle all received messages from the catchup
    for (MessageGeneral msg : messages) {
      boolean enqueue = handleMessage(laoRepository, channel, msg);
      if (enqueue) {
        unprocessed.onNext(genericMessage);
      }
    }
  }

  /**
   * Handle a create LAO request. First create the LAO then subscribe to the LAO channel and finally
   * send a catchup request
   *
   * @param laoRepository     the repository to access the LAOs
   * @param id                the id of the create LAO request
   * @param createLaoRequests the pending create LAO requests
   */
  public static void handleCreateLao(
      LAORepository laoRepository, int id,
      Map<Integer, String> createLaoRequests) {
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

    String consensusChannel = channel + "/consensus";
    laoRepository.sendSubscribe(consensusChannel);
    laoRepository.sendCatchup(consensusChannel);
  }

  /**
   * Send the broadcast messages to the message handler
   *
   * @param genericMessage the generic message received
   * @param unprocessed    the unprocessed messages
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
