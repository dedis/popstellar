package com.github.dedis.popstellar.utility.handler;

import android.util.Log;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddChirp;
import com.github.dedis.popstellar.model.objects.Chirp;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.utility.error.DataHandlingException;
import com.github.dedis.popstellar.utility.error.UnhandledDataTypeException;
import com.github.dedis.popstellar.utility.error.UnknownDataActionException;

/** Chirp messages handler class */
public final class ChirpHandler {

  public static final String TAG = ChirpHandler.class.getSimpleName();

  private ChirpHandler() {
    throw new IllegalArgumentException("Utility class");
  }

  /**
   * Process a Chirp message.
   *
   * @param channel the channel on which the message was received
   * @param data the data of the message that was received
   * @param messageId the ID of the message that was received
   * @param senderPk the public key of the sender of this message
   */
  public static void handleChirpMessage(
      LAORepository laoRepository, String channel, Data data, String messageId, String senderPk)
      throws DataHandlingException {
    Log.d(TAG, "handle Chirp message");

    Action action = Action.find(data.getAction());
    if (action == null) throw new UnknownDataActionException(data);

    switch (action) {
      case ADD:
        handleChirpAdd(laoRepository, channel, (AddChirp) data, messageId, senderPk);
        break;
      default:
        Log.w(TAG, "Invalid action for a chirp object : " + data.getAction());
        throw new UnhandledDataTypeException(data, action.getAction());
    }
  }

  public static void handleChirpAdd(
      LAORepository laoRepository,
      String channel,
      AddChirp addChirp,
      String messageId,
      String senderPk) {
    Lao lao = laoRepository.getLaoByChannel(channel);

    Chirp chirp = new Chirp(messageId);

    chirp.setChannel(channel);
    chirp.setSender(senderPk);
    chirp.setText(addChirp.getText());
    chirp.setTimestamp(addChirp.getTimestamp());
    chirp.setParentId(addChirp.getParentId().orElse(""));

    lao.updateChirp(messageId, chirp);
  }
}
