package com.github.dedis.popstellar.utility.handler.data

import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddReaction
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.DeleteReaction
import com.github.dedis.popstellar.model.objects.Reaction
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.SocialMediaRepository
import com.github.dedis.popstellar.utility.error.InvalidMessageIdException
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import timber.log.Timber
import javax.inject.Inject

/** Reaction to chirps handler class  */
class ReactionHandler @Inject constructor(
    private val laoRepo: LAORepository,
    private val socialMediaRepo: SocialMediaRepository
) {
    /**
     * Process a AddReaction message.
     *
     * @param context the HandlerContext of the message
     * @param addReaction the message that was received
     */
    @Throws(UnknownLaoException::class, InvalidMessageIdException::class)
    fun handleAddReaction(context: HandlerContext, addReaction: AddReaction) {
        val channel = context.channel
        val messageId = context.messageId
        val senderPk = context.senderPk
        Timber.tag(TAG)
            .d("handleAddReaction: channel: %s, chirp id: %s", channel, addReaction.chirpId)
        val laoView = laoRepo.getLaoViewByChannel(channel)
        val reaction = Reaction(
            messageId,
            senderPk,
            addReaction.codepoint,
            addReaction.chirpId,
            addReaction.timestamp
        )
        if (!socialMediaRepo.addReaction(laoView.id, reaction)) {
            throw InvalidMessageIdException(addReaction, addReaction.chirpId)
        }
    }

    /**
     * Process a DeleteReaction message.
     *
     * @param context the HandlerContext of the message
     * @param deleteReaction the message that was received
     */
    @Throws(UnknownLaoException::class, InvalidMessageIdException::class)
    fun handleDeleteReaction(context: HandlerContext, deleteReaction: DeleteReaction) {
        val channel = context.channel
        Timber.tag(TAG)
            .d(
                "handleDeleteReaction: channel: %s, reaction id: %s",
                channel, deleteReaction.reactionID
            )
        val laoView = laoRepo.getLaoViewByChannel(channel)
        if (!socialMediaRepo.deleteReaction(laoView.id, deleteReaction.reactionID)) {
            throw InvalidMessageIdException(deleteReaction, deleteReaction.reactionID)
        }
    }

    companion object {
        val TAG: String = ReactionHandler::class.java.simpleName
    }
}