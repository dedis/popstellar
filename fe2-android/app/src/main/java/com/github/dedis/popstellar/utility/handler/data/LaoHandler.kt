package com.github.dedis.popstellar.utility.handler.data

import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao
import com.github.dedis.popstellar.model.network.method.message.data.lao.GreetLao
import com.github.dedis.popstellar.model.network.method.message.data.lao.StateLao
import com.github.dedis.popstellar.model.network.method.message.data.lao.UpdateLao
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.PendingUpdate
import com.github.dedis.popstellar.model.objects.Server
import com.github.dedis.popstellar.model.objects.WitnessMessage
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.objects.view.LaoView
import com.github.dedis.popstellar.repository.ConsensusRepository
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.MessageRepository
import com.github.dedis.popstellar.repository.ServerRepository
import com.github.dedis.popstellar.repository.WitnessingRepository
import com.github.dedis.popstellar.utility.error.DataHandlingException
import com.github.dedis.popstellar.utility.error.InvalidMessageIdException
import com.github.dedis.popstellar.utility.error.InvalidSignatureException
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.security.KeyManager
import javax.inject.Inject
import timber.log.Timber

/** Lao messages handler class */
class LaoHandler
@Inject
constructor(
    private val keyManager: KeyManager,
    private val messageRepo: MessageRepository,
    private val laoRepo: LAORepository,
    private val serverRepo: ServerRepository,
    private val witnessingRepo: WitnessingRepository,
    private val consensusRepo: ConsensusRepository
) {

  /**
   * Process a CreateLao message.
   *
   * @param context the HandlerContext of the message
   * @param createLao the message that was received
   */
  @Throws(UnknownLaoException::class)
  fun handleCreateLao(context: HandlerContext, createLao: CreateLao) {
    val channel = context.channel
    val witnesses: Set<PublicKey> = HashSet(createLao.witnesses)
    Timber.tag(TAG).d("handleCreateLao: channel: %s, msg: %s", channel, createLao)

    val lao = Lao(createLao)
    laoRepo.updateLao(lao)

    witnessingRepo.addWitnesses(lao.id, witnesses)
    consensusRepo.setOrganizer(lao.id, createLao.organizer)
    consensusRepo.initKeyToNode(lao.id, witnesses)

    val laoView = laoRepo.getLaoViewByChannel(channel)
    val publicKey = keyManager.mainPublicKey
    if (laoView.isOrganizer(publicKey) || witnessingRepo.isWitness(lao.id, publicKey)) {
      laoRepo.addDisposable(
          context.messageSender
              .subscribe(lao.channel.subChannel("consensus"))
              .subscribe( // For now if we receive an error, we assume that it is because the server
                  // running is the scala one which does not implement consensus
                  { Timber.tag(TAG).d("subscription to consensus channel was a success") },
                  { error: Throwable ->
                    Timber.tag(TAG).d(error, "error while trying to subscribe to consensus channel")
                  }))
    }

    laoRepo.addDisposable(
        context.messageSender
            .subscribe(channel.subChannel("federation"))
            .subscribe(
                { Timber.tag(TAG).d("subscription to the federation channel was a success") },
                { error: Throwable ->
                  Timber.tag(TAG).d(error, "error while trying  to subscribe to federation channel")
                }))

    /* Creation channel coin*/
    laoRepo.addDisposable(
        context.messageSender
            .subscribe(channel.subChannel("coin"))
            .subscribe(
                { Timber.tag(TAG).d("subscription to the coin channel was a success") },
                { error: Throwable ->
                  Timber.tag(TAG).d(error, "error while trying  to subscribe to coin channel")
                }))

    consensusRepo.updateNodesByChannel(channel)
  }

  /**
   * Process an UpdateLao message.
   *
   * @param context the HandlerContext of the message
   * @param updateLao the message that was received
   */
  @Throws(DataHandlingException::class, UnknownLaoException::class)
  fun handleUpdateLao(context: HandlerContext, updateLao: UpdateLao) {
    val channel = context.channel
    val messageId = context.messageId
    Timber.tag(TAG).d("Receive Update Lao Broadcast msg: %s", updateLao)

    val laoView = laoRepo.getLaoViewByChannel(channel)
    val laoId = laoView.id
    if (laoView.lastModified > updateLao.lastModified) {
      // the current state we have is more up to date
      throw DataHandlingException(
          updateLao, "The current Lao is more up to date than the update lao message")
    }

    val message: WitnessMessage =
        if (updateLao.name != laoView.name) {
          updateLaoNameWitnessMessage(messageId, updateLao, laoView)
        } else if (witnessingRepo.getWitnesses(laoId) != updateLao.witnesses) {
          updateLaoWitnessesWitnessMessage(messageId, updateLao, laoView)
        } else {
          Timber.tag(TAG).d("Cannot set the witness message title to update lao")
          throw DataHandlingException(
              updateLao, "Cannot set the witness message title to update lao")
        }
    witnessingRepo.addWitnessMessage(laoId, message)

    val lao = laoView.createLaoCopy()
    if (witnessingRepo.areWitnessesEmpty(laoId)) {
      // We send a pending update only if there are already some witness that need to sign this
      // UpdateLao
      lao.addPendingUpdate(PendingUpdate(updateLao.lastModified, messageId))
    }

    consensusRepo.updateNodesByChannel(channel)
    laoRepo.updateLao(lao)
  }

  /**
   * Process a StateLao message.
   *
   * @param context the HandlerContext of the message
   * @param stateLao the message that was received
   */
  @Throws(
      UnknownLaoException::class,
      InvalidMessageIdException::class,
      InvalidSignatureException::class)
  fun handleStateLao(context: HandlerContext, stateLao: StateLao) {
    val channel = context.channel
    val laoView = laoRepo.getLaoViewByChannel(channel)
    Timber.tag(TAG).d("Receive State Lao Broadcast msg: %s, name: %s", stateLao, stateLao.name)

    if (!messageRepo.isMessagePresent(stateLao.modificationId, true)) {
      Timber.tag(TAG).d("Can't find modification id : %s", stateLao.modificationId)
      throw InvalidMessageIdException(stateLao, stateLao.modificationId)
    }

    Timber.tag(TAG).d("Verifying signatures")
    for (pair in stateLao.modificationSignatures) {
      if (!pair.witness.verify(pair.signature, stateLao.modificationId)) {
        throw InvalidSignatureException(stateLao, pair.signature)
      }
    }
    Timber.tag(TAG).d("Success to verify state lao signatures")

    /* TODO: verify if lao/state_lao is consistent with the lao/update message */

    val lao = laoView.createLaoCopy()
    lao.id = stateLao.id
    lao.setName(stateLao.name)
    lao.lastModified = stateLao.lastModified
    lao.modificationId = stateLao.modificationId

    consensusRepo.initKeyToNode(lao.id, stateLao.witnesses)

    val publicKey = keyManager.mainPublicKey
    if (laoView.isOrganizer(publicKey) || witnessingRepo.isWitness(lao.id, publicKey)) {
      laoRepo.addDisposable(
          context.messageSender
              .subscribe(laoView.channel.subChannel("consensus"))
              .subscribe(
                  { Timber.tag(TAG).d("Successful subscribe to consensus channel") },
                  { e: Throwable ->
                    Timber.tag(TAG).d(e, "Unsuccessful subscribe to consensus channel")
                  }))
    }

    // Now we're going to remove all pending updates which came prior to this state lao
    val targetTime = stateLao.lastModified
    lao.pendingUpdates.removeIf { pendingUpdate: PendingUpdate ->
      pendingUpdate.modificationTime <= targetTime
    }

    laoRepo.updateLao(lao)
    consensusRepo.updateNodesByChannel(channel)
  }

  @Throws(UnknownLaoException::class)
  fun handleGreetLao(context: HandlerContext, greetLao: GreetLao) {
    val channel = context.channel
    val laoView = laoRepo.getLaoViewByChannel(channel)
    Timber.tag(TAG).d("handleGreetLao: channel: %s, msg: %s", channel, greetLao)

    // Check the correctness of the LAO id
    if (laoView.id != greetLao.id) {
      Timber.tag(TAG)
          .d(
              "Current lao id %s doesn't match the lao id from greetLao message (%s)",
              laoView.id,
              greetLao.id)
      throw IllegalArgumentException(
          "Current lao doesn't match the lao id from the greetLao message")
    }

    Timber.tag(TAG).d("Creating a server with IP: %s", greetLao.address)
    val server = Server(greetLao.address, greetLao.frontendKey)

    Timber.tag(TAG).d("Adding the server to the repository for lao id : %s", laoView.id)
    serverRepo.addServer(greetLao.id, server)

    // Extend the current connection by connecting to the peers of the main server
    // The greetLao will also be sent by the other servers, so the message sender
    // should handle this, avoiding to connect twice to the same server
    // TODO: Remove the comment when testing for backend is finished ! Maxime @Kaz | May 2024
    // Also, I realised removing this line that no tests are actually testing this part of the
    // code...
    // context.messageSender.extendConnection(greetLao.peers)
  }

  companion object {
    val TAG: String = LaoHandler::class.java.simpleName
    private const val OLD_NAME = "Old Lao Name : "
    private const val NEW_NAME = "New Lao Name : "
    private const val LAO_NAME = "Lao Name : "
    private const val MESSAGE_ID = "Message ID : "
    private const val WITNESS_ID = "New Witness ID : "

    @JvmStatic
    fun updateLaoNameWitnessMessage(
        messageId: MessageID,
        updateLao: UpdateLao,
        laoView: LaoView
    ): WitnessMessage {
      val message = WitnessMessage(messageId)
      message.title = "Update Lao Name "
      message.description =
          "$OLD_NAME\n${laoView.name}\n\n$NEW_NAME\n${updateLao.name}" + "$MESSAGE_ID\n$messageId"

      return message
    }

    @JvmStatic
    fun updateLaoWitnessesWitnessMessage(
        messageId: MessageID,
        updateLao: UpdateLao,
        laoView: LaoView
    ): WitnessMessage {
      val message = WitnessMessage(messageId)
      val tempList: List<PublicKey> = ArrayList(updateLao.witnesses)
      message.title = "Update Lao Witnesses"
      message.description =
          "$LAO_NAME\n${laoView.name}\n\n$WITNESS_ID\n${tempList[tempList.size - 1]}\n\n$MESSAGE_ID\n$messageId"

      return message
    }
  }
}
