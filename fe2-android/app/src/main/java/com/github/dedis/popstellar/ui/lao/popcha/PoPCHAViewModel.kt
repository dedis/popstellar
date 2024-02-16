package com.github.dedis.popstellar.ui.lao.popcha

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.SingleEvent
import com.github.dedis.popstellar.model.network.method.message.data.popcha.PoPCHAAuthentication
import com.github.dedis.popstellar.model.objects.security.AuthToken
import com.github.dedis.popstellar.model.objects.security.Base64URLData
import com.github.dedis.popstellar.model.objects.security.Base64URLData.Companion.encode
import com.github.dedis.popstellar.model.objects.security.PoPToken
import com.github.dedis.popstellar.model.objects.view.LaoView
import com.github.dedis.popstellar.model.qrcode.PoPCHAQRCode
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.RollCallRepository
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.ui.PopViewModel
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningViewModel
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.error.keys.KeyException
import com.github.dedis.popstellar.utility.security.KeyManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.disposables.CompositeDisposable
import java.security.GeneralSecurityException
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class PoPCHAViewModel
@Inject
constructor(
    application: Application,
    /* Dependencies to inject */
    private val laoRepository: LAORepository,
    private val rollCallRepo: RollCallRepository,
    private val networkManager: GlobalNetworkManager,
    private val keyManager: KeyManager
) : AndroidViewModel(application), QRCodeScanningViewModel, PopViewModel {

  override var laoId: String? = null

  val textDisplayed = MutableLiveData<SingleEvent<String>>()
  val isRequestCompleted = MutableLiveData<SingleEvent<Boolean>>()
  private val connecting = AtomicBoolean(false)
  private val disposables = CompositeDisposable()

  override fun onCleared() {
    super.onCleared()
    disposables.dispose()
  }

  fun deactivateRequestCompleted() {
    isRequestCompleted.postValue(SingleEvent(false))
  }

  private fun postTextDisplayed(text: String) {
    textDisplayed.postValue(SingleEvent(text))
  }

  @get:Throws(UnknownLaoException::class)
  private val lao: LaoView
    get() = laoRepository.getLaoView(laoId!!)

  override fun handleData(data: String?) {
    // Don't process another data from the scanner if I'm already trying to connect
    if (connecting.get()) {
      return
    }

    connecting.set(true)
    val popCHAQRCode: PoPCHAQRCode =
        try {
          PoPCHAQRCode(data, laoId!!)
        } catch (e: IllegalArgumentException) {
          Timber.tag(TAG).e(e, "Invalid QRCode PoPCHAData")
          Toast.makeText(
                  getApplication<Application>().applicationContext,
                  R.string.invalid_qrcode_popcha_data,
                  Toast.LENGTH_LONG)
              .show()
          connecting.set(false)
          return
        }

    val token: AuthToken =
        try {
          keyManager.getLongTermAuthToken(laoId!!, popCHAQRCode.clientId)
        } catch (e: KeyException) {
          Timber.tag(TAG).e(e, "Impossible to generate the token")
          connecting.set(false)
          return
        }

    try {
      sendAuthRequest(popCHAQRCode, token)
      postTextDisplayed(popCHAQRCode.toString())
      isRequestCompleted.postValue(SingleEvent(true))
    } catch (e: GeneralSecurityException) {
      Timber.tag(TAG).e(e, "Impossible to sign the token")
      Toast.makeText(
              getApplication<Application>().applicationContext,
              R.string.error_sign_message,
              Toast.LENGTH_LONG)
          .show()
    } catch (e: UnknownLaoException) {
      Timber.tag(TAG).e(e, "Impossible to find the lao")
      Toast.makeText(
              getApplication<Application>().applicationContext,
              R.string.error_no_lao,
              Toast.LENGTH_LONG)
          .show()
    } catch (e: KeyException) {
      Timber.tag(TAG).e(e, "Impossible to get pop token: no roll call exists in the lao")
      Toast.makeText(
              getApplication<Application>().applicationContext,
              R.string.no_rollcall_exception,
              Toast.LENGTH_LONG)
          .show()
    } finally {
      connecting.set(false)
    }
  }

  @Throws(GeneralSecurityException::class, UnknownLaoException::class, KeyException::class)
  private fun sendAuthRequest(popCHAQRCode: PoPCHAQRCode, token: AuthToken) {
    val nonce = encode(popCHAQRCode.nonce)
    val signedToken = token.sign(Base64URLData(nonce))
    val authMessage =
        PoPCHAAuthentication(
            popCHAQRCode.clientId,
            nonce,
            token.publicKey,
            signedToken,
            popCHAQRCode.host,
            popCHAQRCode.state,
            popCHAQRCode.responseMode)
    val channel = lao.channel.subChannel(AUTHENTICATION)

    disposables.add(
        networkManager.messageSender
            .publish(validToken, channel, authMessage)
            .subscribe(
                { Timber.tag(TAG).d("sent the auth message for popcha") },
                { err: Throwable ->
                  Timber.tag(TAG).e(err, "error sending the auth message for popcha")
                }))
  }

  @get:Throws(KeyException::class)
  private val validToken: PoPToken
    get() = keyManager.getValidPoPToken(laoId!!, rollCallRepo.getLastClosedRollCall(laoId!!))

  override val nbScanned: LiveData<Int?>
    get() = // This is useless for the PoPCHA Scanner (we just scan once)
    MutableLiveData(0)

  override fun setPageTitle(title: Int) {
    // Not used for the PopCHA
  }

  companion object {
    private val TAG = PoPCHAViewModel::class.java.simpleName
    const val AUTHENTICATION = "authentication"
  }
}
