package com.github.dedis.popstellar.utility.handler.data;

import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddReaction;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.DeleteReaction;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Reaction;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.SocialMediaRepository;
import com.github.dedis.popstellar.utility.error.InvalidMessageIdException;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;

import javax.inject.Inject;

import timber.log.Timber;

/** Reaction to chirps handler class */
public class ReactionHandler {

  public static final String TAG = ReactionHandler.class.getSimpleName();

  private final LAORepository laoRepo;
  private final SocialMediaRepository socialMediaRepo;

  @Inject
  public ReactionHandler(LAORepository laoRepo, SocialMediaRepository socialMediaRepo) {
    this.laoRepo = laoRepo;
    this.socialMediaRepo = socialMediaRepo;
  }

  /**
   * Process a AddReaction message.
   *
   * @param context the HandlerContext of the message
   * @param addReaction the message that was received
   */
  public void handleAddReaction(HandlerContext context, AddReaction addReaction)
      throws UnknownLaoException, InvalidMessageIdException {
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();
    PublicKey senderPk = context.getSenderPk();

    Timber.tag(TAG)
        .d("handleAddReaction: channel: %s, chirp id: %s", channel, addReaction.getChirpId());
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);

    Reaction reaction =
        new Reaction(
            messageId,
            senderPk,
            addReaction.getCodepoint(),
            addReaction.getChirpId(),
            addReaction.getTimestamp());

    if (!socialMediaRepo.addReaction(laoView.getId(), reaction)) {
      throw new InvalidMessageIdException(addReaction, addReaction.getChirpId());
    }
  }

  /**
   * Process a DeleteReaction message.
   *
   * @param context the HandlerContext of the message
   * @param deleteReaction the message that was received
   */
  public void handleDeleteReaction(HandlerContext context, DeleteReaction deleteReaction)
      throws UnknownLaoException, InvalidMessageIdException {
    Channel channel = context.getChannel();

    Timber.tag(TAG)
        .d(
            "handleDeleteReaction: channel: %s, reaction id: %s",
            channel, deleteReaction.getReactionID());
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);

    if (!socialMediaRepo.deleteReaction(laoView.getId(), deleteReaction.getReactionID())) {
      throw new InvalidMessageIdException(deleteReaction, deleteReaction.getReactionID());
    }
  }
}
