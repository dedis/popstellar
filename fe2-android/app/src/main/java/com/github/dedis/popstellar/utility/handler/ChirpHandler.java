package com.github.dedis.popstellar.utility.handler;

import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddChirp;
import com.github.dedis.popstellar.model.objects.Chirp;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.repository.LAORepository;

/** Chirp messages handler class */
public final class ChirpHandler {

  public static final String TAG = ChirpHandler.class.getSimpleName();

  private ChirpHandler() {
    throw new IllegalArgumentException("Utility class");
  }

  /**
   * Process an AddChirp message.
   *
   * @param laoRepository the repository to access the LAO of the channel
   * @param channel the channel on which the message was received
   * @param addChirp the data of the message that was received
   * @param messageId the ID of the message that was received
   * @param senderPk the public key of the sender of this message
   */
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
