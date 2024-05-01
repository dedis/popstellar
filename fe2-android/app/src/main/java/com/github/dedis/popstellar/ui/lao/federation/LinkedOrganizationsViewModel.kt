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
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningViewModel
import com.github.dedis.popstellar.utility.error.ErrorUtils
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.security.KeyManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Completable
import javax.inject.Inject

@HiltViewModel
class LinkedOrganizationsViewModel
@Inject
constructor(
    application: Application,
    private val laoRepo: LAORepository,
    private val networkManager: GlobalNetworkManager,
    private val keyManager: KeyManager,
) : AndroidViewModel(application), QRCodeScanningViewModel {
  private lateinit var laoId: String
  override val nbScanned = MutableLiveData<Int>()

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

  fun receiveChallenge() {
    TODO("Not yet implemented")
  }

  override fun handleData(data: String?) {
    TODO("Not yet implemented")
  }

  companion object {
    val TAG: String = LinkedOrganizationsViewModel::class.java.simpleName
  }
}
