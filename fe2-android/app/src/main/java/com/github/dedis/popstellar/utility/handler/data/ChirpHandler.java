package com.github.dedis.popstellar.utility.handler.data;

import android.util.Log;

import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddChirp;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.DeleteChirp;
import com.github.dedis.popstellar.model.objects.Chirp;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.LAORepository;

import java.util.Optional;

/** Chirp messages handler class */
public final class ChirpHandler {

  public static final String TAG = ChirpHandler.class.getSimpleName();

  private ChirpHandler() {
    throw new IllegalArgumentException("Utility class");
  }

  /**
   * Process an AddChirp message.
   *
   * @param context the HandlerContext of the message
   * @param addChirp the data of the message that was received
   */
  public static void handleChirpAdd(HandlerContext context, AddChirp addChirp) {
    LAORepository laoRepository = context.getLaoRepository();
    String channel = context.getChannel();
    MessageID messageId = context.getMessageId();
    PublicKey senderPk = context.getSenderPk();

    Lao lao = laoRepository.getLaoByChannel(channel);

    Chirp chirp = new Chirp(messageId);

    chirp.setChannel(channel);
    chirp.setSender(senderPk);
    chirp.setText(addChirp.getText());
    chirp.setTimestamp(addChirp.getTimestamp());
    chirp.setParentId(addChirp.getParentId().orElse(new MessageID("")));

    lao.updateChirp(messageId, chirp);
  }

  /**
   * process a DeleteChirp message.
   *
   * @param context the HandlerContext of the message
   * @param deleteChirp the data of the message that was received
   */
  public static void handleDeleteChirp(HandlerContext context, DeleteChirp deleteChirp) {
    LAORepository laoRepository = context.getLaoRepository();
    String channel = context.getChannel();

    Lao lao = laoRepository.getLaoByChannel(channel);

    Optional<Chirp> chirpOptional = lao.getChirp(deleteChirp.getChirpId());
    Chirp chirp;

    if (!chirpOptional.isPresent()) {
      throw new IllegalArgumentException("The chirp does not exist");
    } else {
      chirp = chirpOptional.get();
    }

    if (chirp.getIsDeleted()) {
      Log.d(TAG, "The chirp is already deleted");
    } else {
      chirp.setIsDeleted(true);
      chirp.setText("");
    }
  }
}
