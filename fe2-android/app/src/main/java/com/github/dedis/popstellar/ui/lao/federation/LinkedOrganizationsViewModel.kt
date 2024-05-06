package com.github.dedis.popstellar.ui.lao.federation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.network.method.message.data.federation.Challenge
import com.github.dedis.popstellar.model.network.method.message.data.federation.ChallengeRequest
import com.github.dedis.popstellar.model.network.method.message.data.federation.FederationExpect
import com.github.dedis.popstellar.model.network.method.message.data.federation.FederationInit
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
  override val nbScanned = MutableLiveData<Int>()

  fun setLaoId(laoId: String?) {
    if (laoId != null) {
      this.laoId = laoId
    }
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
        laoView.channel,
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
    val federationInit = FederationInit(remoteLaoId, serverAddress, publicKey, challenge)
    return networkManager.messageSender.publish(
        keyManager.mainKeyPair,
        laoView.channel,
        federationInit,
    )
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
      challenge: Challenge
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
        laoView.channel,
        federationExpect,
    )
  }

  fun getRepository(): LinkedOrganizationsRepository {
    return linkedOrgRepo
  }

  fun doWhenChallengeIsReceived(function: (Challenge) -> Unit) {
    linkedOrgRepo.setOnChallengeUpdatedCallback(function)
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
              R.string.qr_code_not_federation_details)
          connecting.set(false)
          return
        }

    if (federationDetails.challenge == null) {
      // The federationDetails object is sent by the invitation creator
      disposables.add(
          sendFederationExpect(
                  federationDetails.laoId,
                  federationDetails.serverAddress,
                  federationDetails.publicKey,
                  linkedOrgRepo.getChallenge()!!)
              .subscribe(
                  { linkedOrgRepo.flush() },
                  { error: Throwable ->
                    ErrorUtils.logAndShow(
                        getApplication(), TAG, error, R.string.error_sending_federation_expect)
                  },
              ))
    } else {
      // The federationDetails object is sent by the one who joins the invitation
      linkedOrgRepo.otherLaoId = federationDetails.laoId
      linkedOrgRepo.otherServerAddr = federationDetails.serverAddress
      linkedOrgRepo.otherPublicKey = federationDetails.publicKey
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
  }
}
