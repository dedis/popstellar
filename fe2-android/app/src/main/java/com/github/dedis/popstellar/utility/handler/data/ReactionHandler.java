package com.github.dedis.popstellar.utility.handler.data;

import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddReaction;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.DeleteReaction;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.SocialMediaRepository;

import javax.inject.Inject;

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

  public void handleAddReaction(HandlerContext context, AddReaction addReaction) {}

  public void handleDeleteReaction(HandlerContext context, DeleteReaction deleteReaction) {}
}
