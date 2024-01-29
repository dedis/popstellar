package com.github.dedis.popstellar.ui.lao.witness

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.data.lao.StateLao
import com.github.dedis.popstellar.model.network.method.message.data.lao.UpdateLao
import com.github.dedis.popstellar.model.network.method.message.data.message.WitnessMessageSignature
import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.model.objects.WitnessMessage
import com.github.dedis.popstellar.model.objects.security.KeyPair
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.objects.view.LaoView
import com.github.dedis.popstellar.model.qrcode.MainPublicKeyData
import com.github.dedis.popstellar.model.qrcode.MainPublicKeyData.Companion.extractFrom
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.WitnessingRepository
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningViewModel
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.security.KeyManager
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import java.time.Instant
import java.util.stream.Collectors
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class WitnessingViewModel
@Inject
constructor(
    application: Application,
    private val laoRepo: LAORepository,
    private val witnessingRepo: WitnessingRepository,
    private val keyManager: KeyManager,
    private val networkManager: GlobalNetworkManager,
    private val gson: Gson
) : AndroidViewModel(application), QRCodeScanningViewModel {
  private var laoId: String? = null

  // Scanned witnesses, not necessarily accepted by LAO
  private val scannedWitnesses: MutableSet<PublicKey> = HashSet()

  // Accepted witnesses
  val witnesses = MutableLiveData<List<PublicKey>>()

  // Received witness messages
  val witnessMessages = MutableLiveData<List<WitnessMessage>>()

  override val nbScanned = MutableLiveData(0)

  val showPopup = MutableLiveData(false)
  private val disposables = CompositeDisposable()

  fun setWitnessMessages(messages: List<WitnessMessage>) {
    witnessMessages.value = messages
  }

  val isWitness: Boolean
    get() = witnessingRepo.isWitness(laoId!!, keyManager.mainPublicKey)

  fun setWitnesses(witnesses: List<PublicKey>) {
    this.witnesses.value = witnesses
  }

  fun getScannedWitnesses(): List<PublicKey> {
    return ArrayList(scannedWitnesses)
  }

  /**
   * Function that initializes the view model. It sets the lao identifier and observe the witnesses
   * and witness messages.
   *
   * @param laoId identifier of the lao whose view model belongs
   */
  @Throws(UnknownLaoException::class)
  fun initialize(laoId: String?): WitnessingViewModel {
    this.laoId = laoId ?: throw UnknownLaoException("Null lao id")

    val lao = laoRepo.getLaoView(laoId)
    disposables.addAll( // Observe the witnesses
        witnessingRepo
            .getWitnessesObservableInLao(laoId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { witnessesSet: Set<PublicKey> -> setWitnesses(ArrayList(witnessesSet)) },
                { error: Throwable ->
                  Timber.tag(TAG).e(error, "Error in updating the witnesses of lao %s", laoId)
                }), // Observe the witness messages
        witnessingRepo
            .getWitnessMessagesObservableInLao(laoId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ witnessMessage: List<WitnessMessage> ->
              if (witnessMessage.isEmpty()) {
                return@subscribe
              }
              // Order by latest arrived
              setWitnessMessages(
                  witnessMessage
                      .stream()
                      .sorted(Comparator.comparing(WitnessMessage::timestamp).reversed())
                      .collect(Collectors.toList()))
              val lastMessage = witnessMessages.value!![0]

              // When a new witness message is received, if it needs to be yet signed by the
              // witness
              // we show a pop up that the user can click to open the witnessing fragment.

              // Don't show the pop-up for the organizer as we use an automatic signature
              // mechanism
              val myPk = keyManager.mainPublicKey
              val isOrganizer = lao.isOrganizer(myPk)
              val isWitness = witnessingRepo.isWitness(laoId, myPk)
              val alreadySigned = lastMessage.witnesses.contains(myPk)

              // Allow to sign the message only if the user is a witness and hasn't signed yet
              if (isWitness && !alreadySigned) {
                if (isOrganizer) {
                  // Automatically sign the messages if it's the organizer
                  disposables.add(
                      signMessage(lastMessage)
                          .subscribe(
                              {
                                Timber.tag(TAG)
                                    .d(
                                        "Witness message automatically successfully signed by organizer")
                              },
                              { error: Throwable ->
                                Timber.tag(TAG)
                                    .e(error, "Error signing automatically message from organizer")
                              }))
                } else {
                  showPopup.setValue(true)
                }
              }
            }) { error: Throwable ->
              Timber.tag(TAG).e(error, "Error in updating the witness messages of lao %s", laoId)
            })

    return this
  }

  /**
   * This function deletes the messages that have already passed the witnessing policy to clear
   * useless space.
   */
  fun deleteSignedMessages() {
    Timber.tag(TAG).d("Deleting witnessing messages already signed by enough witnesses")
    witnessingRepo.deleteSignedMessages(laoId!!)
  }

  override fun onCleared() {
    super.onCleared()
    disposables.clear()
  }

  fun disableShowPopup() {
    showPopup.value = false
  }

  fun signMessage(witnessMessage: WitnessMessage): Completable {
    Timber.tag(TAG).d("signing message with ID %s", witnessMessage.messageId)

    val laoView: LaoView =
        try {
          lao
        } catch (e: UnknownLaoException) {
          logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception)
          return Completable.error(UnknownLaoException())
        }

    return Single.fromCallable(keyManager::mainKeyPair).flatMapCompletable { keyPair: KeyPair ->
      // Generate the signature of the message
      val signature = keyPair.sign(witnessMessage.messageId)

      Timber.tag(TAG).d("Signed message id, resulting signature : %s", signature)

      val signatureMessage = WitnessMessageSignature(witnessMessage.messageId, signature)
      networkManager.messageSender.publish(
          keyManager.mainKeyPair, laoView.channel, signatureMessage)
    }
  }

  override fun handleData(data: String?) {
    val pkData: MainPublicKeyData =
        try {
          extractFrom(gson, data)
        } catch (e: Exception) {
          logAndShow(
              getApplication<Application>().applicationContext,
              TAG,
              e,
              R.string.qr_code_not_main_pk)
          return
        }

    val publicKey = pkData.publicKey
    if (scannedWitnesses.contains(publicKey)) {
      logAndShow(getApplication(), TAG, R.string.witness_already_scanned_warning)
      return
    }

    scannedWitnesses.add(publicKey)
    nbScanned.value = scannedWitnesses.size

    Timber.tag(TAG).d("Witness %s successfully scanned", publicKey)

    Toast.makeText(getApplication(), R.string.witness_scan_success, Toast.LENGTH_SHORT).show()
    // So far the update Lao it's not used, we simply add the witnesses at creation time
    /*
    disposables.add(
        updateLaoWitnesses()
            .subscribe(
                () -> {
                  String networkSuccess =
                      String.format(getApplication().getString(R.string.witness_added), publicKey);
                  Timber.tag(TAG).d(networkSuccess);
                  Toast.makeText(getApplication(), networkSuccess, Toast.LENGTH_SHORT).show();
                },
                error -> {
                  scannedWitnesses.remove(publicKey);
                  nbScanned.setValue(scannedWitnesses.size());
                  ErrorUtils.INSTANCE.logAndShow(getApplication(), TAG, error, R.string.error_update_lao);
                }));
     */
  }

  private fun updateLaoWitnesses(): Completable {
    Timber.tag(TAG).d("Updating lao witnesses ")
    val laoView: LaoView =
        try {
          lao
        } catch (e: UnknownLaoException) {
          logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception)
          return Completable.error(UnknownLaoException())
        }

    val channel = laoView.channel
    val mainKey = keyManager.mainKeyPair
    val now = Instant.now().epochSecond
    val updateLao =
        UpdateLao(mainKey.publicKey, laoView.creation, laoView.name!!, now, scannedWitnesses)

    val msg = MessageGeneral(mainKey, updateLao, gson)
    return networkManager.messageSender
        .publish(channel, msg)
        .doOnComplete { Timber.tag(TAG).d("updated lao witnesses") }
        .andThen(dispatchLaoUpdate(updateLao, laoView, channel, msg))
  }

  private fun dispatchLaoUpdate(
      updateLao: UpdateLao,
      laoView: LaoView,
      channel: Channel,
      msg: MessageGeneral
  ): Completable {
    val stateLao =
        StateLao(
            updateLao.id,
            updateLao.name,
            laoView.creation,
            updateLao.lastModified,
            laoView.organizer,
            msg.messageId,
            updateLao.witnesses,
            ArrayList())
    return networkManager.messageSender
        .publish(keyManager.mainKeyPair, channel, stateLao)
        .doOnComplete { Timber.tag(TAG).d("updated lao with %s", stateLao) }
  }

  @get:Throws(UnknownLaoException::class)
  private val lao: LaoView
    get() = laoRepo.getLaoView(laoId!!)

  companion object {
    private val TAG = WitnessingViewModel::class.java.simpleName
  }
}
