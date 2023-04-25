package com.github.dedis.popstellar.utility.handler.data;

import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddChirp;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.DeleteChirp;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Chirp;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.SocialMediaRepository;
import com.github.dedis.popstellar.utility.error.InvalidMessageIdException;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;

import javax.inject.Inject;

import timber.log.Timber;

/** Chirp messages handler class */
public final class ChirpHandler {

  public static final String TAG = ChirpHandler.class.getSimpleName();

  private final LAORepository laoRepo;
  private final SocialMediaRepository socialMediaRepo;

  @Inject
  public ChirpHandler(LAORepository laoRepo, SocialMediaRepository socialMediaRepo) {
    this.laoRepo = laoRepo;
    this.socialMediaRepo = socialMediaRepo;
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

    Timber.tag(TAG).d("handleChirpAdd: channel: %s, id: %s", channel, addChirp.getParentId());
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);
    Chirp chirp =
        new Chirp(
            messageId,
            senderPk,
            addChirp.getText(),
            addChirp.getTimestamp(),
            addChirp.getParentId().orElse(new MessageID("")));

    socialMediaRepo.addChirp(laoView.getId(), chirp);
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

    Timber.tag(TAG).d("handleDeleteChirp: channel: %s, id: %s", channel, deleteChirp.getChirpId());

    LaoView laoView = laoRepo.getLaoViewByChannel(channel);
    boolean chirpExist = socialMediaRepo.deleteChirp(laoView.getId(), deleteChirp.getChirpId());
    if (!chirpExist) {
      throw new InvalidMessageIdException(deleteChirp, deleteChirp.getChirpId());
    }
  }
}
