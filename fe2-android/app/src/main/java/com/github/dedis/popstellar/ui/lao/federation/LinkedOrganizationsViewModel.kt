package com.github.dedis.popstellar.ui.lao.federation

import android.app.Application
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.data.federation.Challenge
import com.github.dedis.popstellar.model.network.method.message.data.federation.ChallengeRequest
import com.github.dedis.popstellar.model.network.method.message.data.federation.FederationExpect
import com.github.dedis.popstellar.model.network.method.message.data.federation.FederationInit
import com.github.dedis.popstellar.model.network.method.message.data.federation.TokensExchange
import com.github.dedis.popstellar.model.objects.view.LaoView
import com.github.dedis.popstellar.model.qrcode.FederationDetails
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.LinkedOrganizationsRepository
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningViewModel
import com.github.dedis.popstellar.utility.error.ErrorUtils
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.security.KeyManager
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class LinkedOrganizationsViewModel
@Inject
constructor(
    application: Application,
    private val laoRepo: LAORepository,
    private val linkedOrgRepo: LinkedOrganizationsRepository,
    private val networkManager: GlobalNetworkManager,
    private val keyManager: KeyManager,
    private val gson: Gson,
) : AndroidViewModel(application), QRCodeScanningViewModel {
  private lateinit var laoId: String
  private val connecting = AtomicBoolean(false)
  private val disposables = CompositeDisposable()
  var manager: FragmentManager? = null
  override val nbScanned = MutableLiveData<Int>()

  fun setLaoId(laoId: String) {
    this.laoId = laoId
  }

  /**
   * Sends a Challenge Request data
   *
   * @param timestamp time of the Challenge Request
   */
  fun sendChallengeRequest(timestamp: Long): Completable {
    val laoView: LaoView =
        try {
          laoRepo.getLaoView(laoId)
        } catch (e: UnknownLaoException) {
          ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception)
          return Completable.error(UnknownLaoException())
        }
    val challengeRequest = ChallengeRequest(timestamp)
    return networkManager.messageSender.publish(
        keyManager.mainKeyPair,
        laoView.channel.subChannel(FEDERATION),
        challengeRequest,
    )
  }

  /**
   * Sends a Federation Init data
   *
   * @param remoteLaoId ID of the remote LAO
   * @param serverAddress public address of the remote organizer server
   * @param publicKey public key of the remote organizer
   * @param challenge challenge from the other server
   */
  fun sendFederationInit(
      remoteLaoId: String,
      serverAddress: String,
      publicKey: String,
      challenge: Challenge
  ): Completable {
    val laoView: LaoView =
        try {
          laoRepo.getLaoView(laoId)
        } catch (e: UnknownLaoException) {
          ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception)
          return Completable.error(UnknownLaoException())
        }
    val federationInit =
        FederationInit(
            remoteLaoId, serverAddress, publicKey, getMessageGeneralFromChallenge(challenge))
    return networkManager.messageSender.publish(
        keyManager.mainKeyPair,
        laoView.channel.subChannel(FEDERATION),
        federationInit,
    )
  }

  /** Sends a Federation Init data with parameters from the repository */
  fun sendFederationInitFromRepository(): Completable {
    return sendFederationInit(
        linkedOrgRepo.otherLaoId!!,
        linkedOrgRepo.otherServerAddr!!,
        linkedOrgRepo.otherPublicKey!!,
        linkedOrgRepo.getChallenge()!!)
  }

  /**
   * Sends a Federation Expect data
   *
   * @param remoteLaoId ID of the remote LAO
   * @param serverAddress public address of the remote organizer server
   * @param publicKey public key of the remote organizer
   * @param challenge challenge for the server
   */
  fun sendFederationExpect(
      remoteLaoId: String,
      serverAddress: String,
      publicKey: String,
      challenge: MessageGeneral
  ): Completable {
    val laoView: LaoView =
        try {
          laoRepo.getLaoView(laoId)
        } catch (e: UnknownLaoException) {
          ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception)
          return Completable.error(UnknownLaoException())
        }
    val federationExpect = FederationExpect(remoteLaoId, serverAddress, publicKey, challenge)
    return networkManager.messageSender.publish(
        keyManager.mainKeyPair,
        laoView.channel.subChannel(FEDERATION),
        federationExpect,
    )
  }

  /**
   * Sends a Token Exchange data
   *
   * @param remoteLaoId ID of the remote LAO
   * @param rollCallId ID of the rollCall of the remote LAO
   * @param attendees array with the token of each attendee
   */
  fun sendTokensExchange(
      remoteLaoId: String,
      rollCallId: String,
      attendees: Array<String>
  ): Completable {
    val laoView: LaoView =
        try {
          laoRepo.getLaoView(laoId)
        } catch (e: UnknownLaoException) {
          ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception)
          return Completable.error(UnknownLaoException())
        }
    val timestamp = Instant.now().epochSecond
    val tokensExchange = TokensExchange(remoteLaoId, rollCallId, attendees, timestamp)
    return networkManager.messageSender.publish(
        keyManager.mainKeyPair, laoView.channel.subChannel(FEDERATION), tokensExchange)
  }

  fun getChallenge(): Challenge? {
    return linkedOrgRepo.getChallenge()
  }

  fun getLinkedLaosMap(): Map<String, Array<String>> {
    return linkedOrgRepo.getLinkedLaos(laoId)
  }

  fun isRepositoryValid(): Boolean {
    return linkedOrgRepo.otherLaoId != null &&
        linkedOrgRepo.otherServerAddr != null &&
        linkedOrgRepo.otherPublicKey != null
  }

  fun flushRepository() {
    linkedOrgRepo.flush()
  }

  private fun getMessageGeneralFromChallenge(challenge: Challenge): MessageGeneral {
    return MessageGeneral(keyManager.mainKeyPair, challenge, gson)
  }

  fun doWhenChallengeIsReceived(function: (Challenge) -> Unit) {
    linkedOrgRepo.setOnChallengeUpdatedCallback(function)
  }

  fun doWhenLinkedLaosIsUpdated(function: (String, MutableMap<String, Array<String>>) -> Unit) {
    linkedOrgRepo.setOnLinkedLaosUpdatedCallback(function)
  }

  fun setLinkedLaosNotifyFunction() {
    linkedOrgRepo.setNewTokensNotifyFunction { receivedLaoId, otherLaoId, rollCallId, tokens ->
      if (receivedLaoId == laoId) {
        disposables.add(
            sendTokensExchange(otherLaoId, rollCallId, tokens)
                .subscribe(
                    { ErrorUtils.logAndShow(getApplication(), TAG, R.string.tokens_exchange_sent) },
                    { error: Throwable ->
                      ErrorUtils.logAndShow(
                          getApplication(), TAG, error, R.string.error_sending_tokens_exchange)
                    },
                ))
      }
    }
  }

  override fun handleData(data: String?) {
    // Don't process another data from the scanner if I'm already trying to scan
    if (connecting.get()) {
      return
    }

    connecting.set(true)
    val federationDetails: FederationDetails =
        try {
          FederationDetails.extractFrom(gson, data)
        } catch (e: Exception) {
          ErrorUtils.logAndShow(
              getApplication<Application>().applicationContext,
              TAG,
              e,
              R.string.qr_code_not_federation_details)
          connecting.set(false)
          return
        }

    // Saving the other organization details to the repository
    linkedOrgRepo.otherLaoId = federationDetails.laoId
    linkedOrgRepo.otherServerAddr = federationDetails.serverAddress
    linkedOrgRepo.otherPublicKey = federationDetails.publicKey

    if (federationDetails.challenge == null) {
      // The federationDetails object is sent by the invitation creator
      disposables.add(
          sendFederationExpect(
                  federationDetails.laoId,
                  federationDetails.serverAddress,
                  federationDetails.publicKey,
                  getMessageGeneralFromChallenge(linkedOrgRepo.getChallenge()!!))
              .subscribe(
                  { ErrorUtils.logAndShow(getApplication(), TAG, R.string.expect_sent) },
                  { error: Throwable ->
                    ErrorUtils.logAndShow(
                        getApplication(), TAG, error, R.string.error_sending_federation_expect)
                  },
              ))
    } else {
      // The federationDetails object is sent by the one who joins the invitation
      linkedOrgRepo.updateChallenge(federationDetails.challenge)
    }
    connecting.set(false)
  }

  override fun onCleared() {
    super.onCleared()
    disposables.dispose()
  }

  companion object {
    val TAG: String = LinkedOrganizationsViewModel::class.java.simpleName
    val FEDERATION = "federation"
  }
}
