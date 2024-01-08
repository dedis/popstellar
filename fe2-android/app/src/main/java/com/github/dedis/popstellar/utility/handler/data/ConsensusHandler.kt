package com.github.dedis.popstellar.utility.handler.data

import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElect
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElectAccept
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusFailure
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusLearn
import com.github.dedis.popstellar.model.objects.ElectInstance
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.WitnessingRepository
import com.github.dedis.popstellar.utility.error.DataHandlingException
import com.github.dedis.popstellar.utility.error.InvalidMessageIdException
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import javax.inject.Inject
import timber.log.Timber

class ConsensusHandler
@Inject
constructor(private val laoRepo: LAORepository, private val witnessingRepo: WitnessingRepository) {

  /**
   * Process an Elect message.
   *
   * @param context the HandlerContext of the message
   * @param consensusElect the data of the message that was received
   */
  @Throws(UnknownLaoException::class)
  fun handleElect(context: HandlerContext, consensusElect: ConsensusElect) {
    val channel = context.channel
    val messageId = context.messageId
    val senderPk = context.senderPk
    Timber.tag(TAG).d("handleElect: channel: %s, id: %s", channel, consensusElect.instanceId)

    val laoView = laoRepo.getLaoViewByChannel(channel)
    val nodes = witnessingRepo.getWitnesses(laoView.id)
    nodes.add(laoView.organizer)
    val electInstance = ElectInstance(messageId, channel, senderPk, nodes, consensusElect)
    val lao = laoView.createLaoCopy()

    lao.updateElectInstance(electInstance)
    laoRepo.updateLao(lao)
    laoRepo.updateNodes(laoView.channel)
  }

  @Throws(DataHandlingException::class, UnknownLaoException::class)
  fun handleElectAccept(context: HandlerContext, consensusElectAccept: ConsensusElectAccept) {
    val channel = context.channel
    val messageId = context.messageId
    val senderPk = context.senderPk
    Timber.tag(TAG)
        .d("handleElectAccept: channel: %s, id: %s", channel, consensusElectAccept.instanceId)

    val laoView = laoRepo.getLaoViewByChannel(channel)
    val electInstanceOpt = laoView.getElectInstance(consensusElectAccept.messageId)
    if (!electInstanceOpt.isPresent) {
      Timber.tag(TAG).w("elect_accept for invalid messageId : %s", consensusElectAccept.messageId)
      throw InvalidMessageIdException(consensusElectAccept, consensusElectAccept.messageId)
    }
    val electInstance = electInstanceOpt.get()
    electInstance.addElectAccept(senderPk, messageId, consensusElectAccept)
    val lao = laoView.createLaoCopy()

    lao.updateElectInstance(electInstance)
    laoRepo.updateLao(lao)
    laoRepo.updateNodes(laoView.channel)
  }

  @Suppress("unused")
  fun <T : Data> handleBackend(context: HandlerContext, data: T) {
    Timber.tag(TAG).w("Received a consensus message only for backend with action: %s", data.action)
  }

  @Throws(DataHandlingException::class, UnknownLaoException::class)
  fun handleLearn(context: HandlerContext, consensusLearn: ConsensusLearn) {
    val channel = context.channel
    Timber.tag(TAG).d("handleLearn: channel: %s, id: %s", channel, consensusLearn.instanceId)

    val laoView = laoRepo.getLaoViewByChannel(channel)
    val electInstanceOpt = laoView.getElectInstance(consensusLearn.messageId)
    if (!electInstanceOpt.isPresent) {
      Timber.tag(TAG).w("learn for invalid messageId : %s", consensusLearn.messageId)
      throw InvalidMessageIdException(consensusLearn, consensusLearn.messageId)
    }
    val electInstance = electInstanceOpt.get()
    if (consensusLearn.learnValue.isDecision) {
      electInstance.state = ElectInstance.State.ACCEPTED
    }
    val lao = laoView.createLaoCopy()

    lao.updateElectInstance(electInstance)
    laoRepo.updateLao(lao)
    laoRepo.updateNodes(laoView.channel)
  }

  @Throws(UnknownLaoException::class, InvalidMessageIdException::class)
  fun handleConsensusFailure(context: HandlerContext, failure: ConsensusFailure) {
    val channel = context.channel
    Timber.tag(TAG).d("handleConsensusFailure: channel: %s, id: %s", channel, failure.instanceId)

    val laoView = laoRepo.getLaoViewByChannel(channel)
    val electInstanceOpt = laoView.getElectInstance(failure.messageId)
    if (!electInstanceOpt.isPresent) {
      Timber.tag(TAG).w("Failure for invalid messageId : %s", failure.messageId)
      throw InvalidMessageIdException(failure, failure.messageId)
    }
    val electInstance = electInstanceOpt.get()
    electInstance.state = ElectInstance.State.FAILED
    val lao = laoView.createLaoCopy()

    lao.updateElectInstance(electInstance)
    laoRepo.updateLao(lao)
    laoRepo.updateNodes(laoView.channel)
  }

  companion object {
    val TAG: String = ConsensusHandler::class.java.simpleName
  }
}
