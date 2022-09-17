package com.github.dedis.popstellar.utility.handler.data;

import android.util.Log;

import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddChirp;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.DeleteChirp;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.utility.error.InvalidMessageIdException;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;

import java.util.Optional;

import javax.inject.Inject;

/** Chirp messages handler class */
public final class ChirpHandler {

  public static final String TAG = ChirpHandler.class.getSimpleName();

  private final LAORepository laoRepo;

  @Inject
  public ChirpHandler(LAORepository laoRepo) {
    this.laoRepo = laoRepo;
  }

  /**
   * Process an AddChirp message.
   *
   * @param context the HandlerContext of the message
   * @param addChirp the data of the message that was received
   */
  public void handleChirpAdd(HandlerContext context, AddChirp addChirp) throws UnknownLaoException {
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();
    PublicKey senderPk = context.getSenderPk();

    Log.d(TAG, "handleChirpAdd: " + channel + " id " + addChirp.getParentId());
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);
    Chirp chirp =
        new Chirp(
            messageId,
            senderPk,
            addChirp.getText(),
            addChirp.getTimestamp(),
            addChirp.getParentId().orElse(new MessageID("")));

    Lao lao = laoView.createLaoCopy();
    lao.updateChirpList(messageId, chirp);
    laoRepo.updateLao(lao);
  }

  /**
   * process a DeleteChirp message.
   *
   * @param context the HandlerContext of the message
   * @param deleteChirp the data of the message that was received
   */
  public void handleDeleteChirp(HandlerContext context, DeleteChirp deleteChirp)
      throws UnknownLaoException, InvalidMessageIdException {
    Channel channel = context.getChannel();

    Log.d(TAG, "handleDeleteChirp: " + channel + " id " + deleteChirp.getChirpId());
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);
    Optional<Chirp> chirpOptional = laoView.getChirp(deleteChirp.getChirpId());

    if (!chirpOptional.isPresent()) {
      throw new InvalidMessageIdException(deleteChirp, deleteChirp.getChirpId());
    }
    Chirp chirp = chirpOptional.get();

    if (chirp.isDeleted()) {
      Log.d(TAG, "The chirp is already deleted");
      return;
    }

    Lao lao = laoView.createLaoCopy();
    lao.updateChirpList(chirp.getId(), chirp.deleted());
    laoRepo.updateLao(lao);
  }
}
