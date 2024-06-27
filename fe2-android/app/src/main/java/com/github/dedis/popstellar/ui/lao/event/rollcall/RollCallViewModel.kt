package com.github.dedis.popstellar.ui.lao.event.rollcall

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CloseRollCall
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CreateRollCall
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.OpenRollCall
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.model.objects.Wallet
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.objects.view.LaoView
import com.github.dedis.popstellar.model.qrcode.PopTokenData
import com.github.dedis.popstellar.model.qrcode.PopTokenData.Companion.extractFrom
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.RollCallRepository
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningViewModel
import com.github.dedis.popstellar.utility.error.DoubleOpenedRollCallException
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.error.UnknownRollCallException
import com.github.dedis.popstellar.utility.error.keys.KeyException
import com.github.dedis.popstellar.utility.error.keys.KeyGenerationException
import com.github.dedis.popstellar.utility.error.keys.UninitializedWalletException
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider
import com.github.dedis.popstellar.utility.security.KeyManager
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.time.Instant
import java.util.ArrayList
import java.util.TreeSet
import java.util.stream.Collectors
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class RollCallViewModel
@Inject
constructor(
    application: Application,
    private val laoRepo: LAORepository,
    private val rollCallRepo: RollCallRepository,
    private val networkManager: GlobalNetworkManager,
    private val keyManager: KeyManager,
    private val wallet: Wallet,
    private val schedulerProvider: SchedulerProvider,
    private val gson: Gson
) : AndroidViewModel(application), QRCodeScanningViewModel {
  private lateinit var laoId: String

  private val attendees: TreeSet<PublicKey> = TreeSet(compareBy { it.toString() })
  override val nbScanned = MutableLiveData<Int>()
  lateinit var attendedRollCalls: Observable<List<RollCall>>
    private set

  fun setLaoId(laoId: String?) {
    if (laoId != null) {
      this.laoId = laoId
      attendedRollCalls =
          rollCallRepo.getRollCallsObservableInLao(laoId).map { rcs: Set<RollCall> ->
            rcs.stream() // Keep only attended roll calls
                .filter { rollcall: RollCall -> isRollCallAttended(rollcall) }
                .collect(Collectors.toList())
          }
    }
  }

  /**
   * Creates new roll call event.
   *
   * Publish a GeneralMessage containing CreateRollCall data.
   *
   * @param title the title of the roll call
   * @param description the description of the roll call, can be empty
   * @param location the location of the roll call
   * @param creation the creation time of the roll call
   * @param proposedStart the proposed start time of the roll call
   * @param proposedEnd the proposed end time of the roll call
   * @return A Single emitting the id of the created rollcall
   */
  fun createNewRollCall(
      title: String,
      description: String?,
      location: String,
      creation: Long,
      proposedStart: Long,
      proposedEnd: Long
  ): Single<String> {
    Timber.tag(TAG).d("creating a new roll call with title %s", title)

    val laoView: LaoView =
        try {
          lao
        } catch (e: UnknownLaoException) {
          logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception)
          return Single.error(UnknownLaoException())
        }

    val createRollCall =
        CreateRollCall(
            title,
            creation,
            proposedStart,
            proposedEnd,
            location,
            description?.ifEmpty { null },
            laoView.id)

    return networkManager.messageSender
        .publish(keyManager.mainKeyPair, laoView.channel, createRollCall)
        .toSingleDefault(createRollCall.id)
  }

  /**
   * Opens a roll call event.
   *
   * Publish a GeneralMessage containing OpenRollCall data.
   *
   * @param id the roll call id to open
   */
  fun openRollCall(id: String): Completable {
    Timber.tag(TAG).d("call openRollCall with id %s", id)

    val laoView: LaoView =
        try {
          lao
        } catch (e: UnknownLaoException) {
          logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception)
          return Completable.error(UnknownLaoException())
        }

    if (!rollCallRepo.canOpenRollCall(laoId)) {
      Timber.tag(TAG)
          .d(
              "failed to open roll call with id %s because another roll call was already opened, laoID: %s",
              id,
              laoView.id)
      return Completable.error(DoubleOpenedRollCallException(id))
    }

    val openedAt = Instant.now().epochSecond
    val rollCall: RollCall =
        try {
          Timber.tag(TAG).d("failed to retrieve roll call with id %s, laoID: %s", id, laoView.id)
          rollCallRepo.getRollCallWithId(laoId, id)
        } catch (e: UnknownRollCallException) {
          return Completable.error(UnknownRollCallException(id))
        }

    val openRollCall = OpenRollCall(laoView.id, id, openedAt, rollCall.state)
    val channel = laoView.channel

    return networkManager.messageSender
        .publish(keyManager.mainKeyPair, channel, openRollCall)
        .doOnComplete { openRollCall(openRollCall.updateId, laoView, rollCall) }
  }

  private fun openRollCall(currentId: String, laoView: LaoView, rollCall: RollCall) {
    Timber.tag(TAG).d("opening rollcall with id %s", currentId)

    attendees.addAll(rollCall.attendees)
    try {
      attendees.add(keyManager.getPoPToken(laoView, rollCall).publicKey)
    } catch (e: KeyException) {
      logAndShow(getApplication(), TAG, e, R.string.error_retrieve_own_token)
    }

    // this to display the initial number of attendees
    nbScanned.postValue(attendees.size)
  }

  /**
   * Closes the roll call event currently open
   *
   * Publish a GeneralMessage containing CloseRollCall data.
   */
  fun closeRollCall(id: String): Completable {
    Timber.tag(TAG).d("call closeRollCall")

    val laoView: LaoView =
        try {
          lao
        } catch (e: UnknownLaoException) {
          logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception)
          return Completable.error(UnknownLaoException())
        }

    val end = Instant.now().epochSecond
    val channel = laoView.channel
    val closeRollCall = CloseRollCall(laoView.id, id, end, ArrayList(attendees))

    return networkManager.messageSender
        .publish(keyManager.mainKeyPair, channel, closeRollCall)
        .doOnComplete {
          Timber.tag(TAG).d("closed the roll call with id %s", id)
          attendees.clear()
        }
  }

  fun getRollCallObservable(persistentId: String): Observable<RollCall> {
    return try {
      rollCallRepo
          .getRollCallObservable(laoId, persistentId)
          .observeOn(schedulerProvider.mainThread())
    } catch (e: UnknownRollCallException) {
      Observable.error(UnknownRollCallException(persistentId))
    }
  }

  @get:Throws(UnknownLaoException::class)
  private val lao: LaoView
    get() = laoRepo.getLaoView(laoId)

  /**
   * Predicate used for filtering rollcalls to make sure that the user either attended the rollcall
   * or was the organizer
   *
   * @param rollcall the roll-call considered
   * @return boolean saying whether user attended or organized the given roll call
   */
  private fun isRollCallAttended(rollcall: RollCall): Boolean {
    // find out if user has attended the rollcall
    return try {
      val isOrganizer = laoRepo.getLaoView(laoId).organizer == keyManager.mainPublicKey
      val pk = wallet.generatePoPToken(laoId, rollcall.persistentId).publicKey
      rollcall.attendees.contains(pk) || isOrganizer
    } catch (e: Exception) {
      when (e) {
        is KeyGenerationException,
        is UninitializedWalletException -> {
          logAndShow(getApplication(), TAG, e, R.string.key_generation_exception)
        }
        is UnknownLaoException -> {
          logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception)
        }
        else -> throw e
      }
      false
    }
  }

  override fun handleData(data: String?) {
    val tokenData: PopTokenData =
        try {
          extractFrom(gson, data)
        } catch (e: Exception) {
          logAndShow(
              getApplication<Application>().applicationContext, TAG, R.string.qr_code_not_pop_token)
          return
        }

    val publicKey = tokenData.popToken
    if (attendees.contains(publicKey)) {
      logAndShow(getApplication(), TAG, R.string.attendee_already_scanned_warning)
      return
    }

    attendees.add(publicKey)

    Timber.tag(TAG).d("Attendee %s successfully added", publicKey)
    Toast.makeText(getApplication(), R.string.attendee_scan_success, Toast.LENGTH_SHORT).show()

    nbScanned.postValue(attendees.size)
  }

  @Inject
  fun getAttendees(): Set<PublicKey> {
    return attendees
  }

  companion object {
    val TAG: String = RollCallViewModel::class.java.simpleName
  }
}
