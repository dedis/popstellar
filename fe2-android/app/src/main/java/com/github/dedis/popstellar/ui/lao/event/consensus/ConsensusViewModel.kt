package com.github.dedis.popstellar.ui.lao.event.consensus

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElect
import com.github.dedis.popstellar.model.network.method.message.data.consensus.ConsensusElectAccept
import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.model.objects.Channel.Companion.getLaoChannel
import com.github.dedis.popstellar.model.objects.ConsensusNode
import com.github.dedis.popstellar.model.objects.ElectInstance
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.repository.ConsensusRepository
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.utility.security.KeyManager
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class ConsensusViewModel
@Inject
constructor(
    application: Application,
    private val consensusRepo: ConsensusRepository,
    private val networkManager: GlobalNetworkManager,
    private val keyManager: KeyManager,
    private val gson: Gson
) : AndroidViewModel(application) {
  private lateinit var laoId: String

  /**
   * Sends a ConsensusElect message.
   *
   * Publish a GeneralMessage containing ConsensusElect data.
   *
   * @param creation the creation time of the consensus
   * @param objId the id of the object the consensus refers to (e.g. election_id)
   * @param type the type of object the consensus refers to (e.g. election)
   * @param property the property the value refers to (e.g. "state")
   * @param value the proposed new value for the property (e.g. "started")
   * @return A single emitting the published message
   */
  fun sendConsensusElect(
      creation: Long,
      objId: String,
      type: String,
      property: String,
      value: Any
  ): Single<MessageGeneral?> {
    Timber.tag(TAG)
        .d("creating a new consensus for type: %s, property: %s, value: %s", type, property, value)

    val channel = getLaoChannel(laoId).subChannel("consensus")
    val consensusElect = ConsensusElect(creation, objId, type, property, value)
    val msg = MessageGeneral(keyManager.mainKeyPair, consensusElect, gson)
    return networkManager.messageSender.publish(channel, msg).toSingleDefault(msg)
  }

  /**
   * Sends a ConsensusElectAccept message.
   *
   * Publish a GeneralMessage containing ConsensusElectAccept data.
   *
   * @param electInstance the corresponding ElectInstance
   * @param accept true if accepted, false if rejected
   */
  fun sendConsensusElectAccept(electInstance: ElectInstance, accept: Boolean): Completable {
    val messageId = electInstance.messageId
    Timber.tag(TAG)
        .d(
            "sending a new elect_accept for consensus with messageId : %s with value %s",
            messageId,
            accept)

    val consensusElectAccept = ConsensusElectAccept(electInstance.instanceId, messageId, accept)
    return networkManager.messageSender.publish(
        keyManager.mainKeyPair, electInstance.channel, consensusElectAccept)
  }

  fun setLaoId(laoId: String) {
    this.laoId = laoId
  }

  fun getNodesByChannel(laoChannel: Channel): Observable<List<ConsensusNode>> {
    return consensusRepo.getNodesByChannel(laoChannel)
  }

  fun getNodeByLao(laoId: String, publicKey: PublicKey): ConsensusNode? {
    return consensusRepo.getNodeByLao(laoId, publicKey)
  }

  companion object {
    private val TAG = ConsensusViewModel::class.java.simpleName
  }
}
