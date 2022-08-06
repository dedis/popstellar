package com.github.dedis.popstellar.utility.handler.data;

import android.util.Log;

import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddChirp;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.DeleteChirp;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.utility.error.DataHandlingException;
import com.github.dedis.popstellar.utility.error.InvalidMessageIdException;

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
  public static void handleChirpAdd(HandlerContext context, AddChirp addChirp)
      throws DataHandlingException {
    LAORepository laoRepository = context.getLaoRepository();
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();
    PublicKey senderPk = context.getSenderPk();

    Log.d(TAG, "handleChirpAdd: " + channel + " id " + addChirp.getParentId());
    Optional<LaoView> laoViewOptional = laoRepository.getLaoViewByChannel(channel);
    if (!laoViewOptional.isPresent()) {
      throw new DataHandlingException(addChirp, "Unknown LAO");
    }
    LaoView laoView = laoViewOptional.get();
    Chirp chirp = new Chirp(messageId);

    chirp.setChannel(channel);
    chirp.setSender(senderPk);
    chirp.setText(addChirp.getText());
    chirp.setTimestamp(addChirp.getTimestamp());
    chirp.setParentId(addChirp.getParentId().orElse(new MessageID("")));

    Lao lao = laoView.getLao();
    lao.updateAllChirps(messageId, chirp);
    laoRepository.updateLao(lao);
  }

  /**
   * process a DeleteChirp message.
   *
   * @param context the HandlerContext of the message
   * @param deleteChirp the data of the message that was received
   */
  public static void handleDeleteChirp(HandlerContext context, DeleteChirp deleteChirp)
      throws DataHandlingException {
    LAORepository laoRepository = context.getLaoRepository();
    Channel channel = context.getChannel();

    Log.d(TAG, "handleDeleteChirp: " + channel + " id " + deleteChirp.getChirpId());
    Optional<LaoView> laoViewOptional = laoRepository.getLaoViewByChannel(channel);
    if (!laoViewOptional.isPresent()) {
      throw new DataHandlingException(deleteChirp, "Unknown LAO");
    }
    LaoView laoView = laoViewOptional.get();
    Optional<Chirp> chirpOptional = laoView.getChirp(deleteChirp.getChirpId());

    if (!chirpOptional.isPresent()) {
      throw new InvalidMessageIdException(deleteChirp, deleteChirp.getChirpId());
    }
    Chirp chirp = chirpOptional.get();

    if (chirp.getIsDeleted()) {
      Log.d(TAG, "The chirp is already deleted");
    } else {
      chirp.setIsDeleted(true);
      chirp.setText("");
    }

    Lao lao = laoView.getLao();
    lao.updateAllChirps(chirp.getId(), chirp);
    laoRepository.updateLao(lao);
  }
}
