package com.github.dedis.popstellar.utility.handler.data

import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddChirp
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.DeleteChirp
import com.github.dedis.popstellar.model.objects.Chirp
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.SocialMediaRepository
import com.github.dedis.popstellar.utility.error.InvalidMessageIdException
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import javax.inject.Inject
import timber.log.Timber

/** Chirp messages handler class */
class ChirpHandler
@Inject
constructor(
    private val laoRepo: LAORepository,
    private val socialMediaRepo: SocialMediaRepository
) {
  /**
   * Process an AddChirp message.
   *
   * @param context the HandlerContext of the message
   * @param addChirp the data of the message that was received
   */
  @Throws(UnknownLaoException::class)
  fun handleChirpAdd(context: HandlerContext, addChirp: AddChirp) {
    val channel = context.channel
    val messageId = context.messageId
    val senderPk = context.senderPk
    Timber.tag(TAG).d("handleChirpAdd: channel: %s, id: %s", channel, addChirp.parentId)
    val laoView = laoRepo.getLaoViewByChannel(channel)
    val chirp =
        Chirp(
            messageId,
            senderPk,
            addChirp.text,
            addChirp.timestamp,
            addChirp.parentId.orElse(MessageID("")))
    socialMediaRepo.addChirp(laoView.id, chirp)
  }

  /**
   * process a DeleteChirp message.
   *
   * @param context the HandlerContext of the message
   * @param deleteChirp the data of the message that was received
   */
  @Throws(UnknownLaoException::class, InvalidMessageIdException::class)
  fun handleDeleteChirp(context: HandlerContext, deleteChirp: DeleteChirp) {
    val channel = context.channel
    Timber.tag(TAG).d("handleDeleteChirp: channel: %s, id: %s", channel, deleteChirp.chirpId)
    val laoView = laoRepo.getLaoViewByChannel(channel)
    val chirpExist = socialMediaRepo.deleteChirp(laoView.id, deleteChirp.chirpId)
    if (!chirpExist) {
      throw InvalidMessageIdException(deleteChirp, deleteChirp.chirpId)
    }
  }

  companion object {
    val TAG: String = ChirpHandler::class.java.simpleName
  }
}
