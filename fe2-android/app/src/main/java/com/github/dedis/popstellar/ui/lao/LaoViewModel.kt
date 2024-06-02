package com.github.dedis.popstellar.ui.lao

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.Role
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.model.objects.Wallet
import com.github.dedis.popstellar.model.objects.security.PoPToken
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.model.objects.view.LaoView
import com.github.dedis.popstellar.repository.ConnectivityRepository
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.RollCallRepository
import com.github.dedis.popstellar.repository.WitnessingRepository
import com.github.dedis.popstellar.repository.database.AppDatabase
import com.github.dedis.popstellar.repository.database.subscriptions.SubscriptionsDao
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.ui.PopViewModel
import com.github.dedis.popstellar.ui.home.ConnectingActivity.Companion.newIntentForJoiningDetail
import com.github.dedis.popstellar.utility.ActivityUtils.saveSubscriptionsRoutine
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.error.keys.KeyException
import com.github.dedis.popstellar.utility.error.keys.KeyGenerationException
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException
import com.github.dedis.popstellar.utility.error.keys.UninitializedWalletException
import com.github.dedis.popstellar.utility.security.KeyManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import javax.inject.Inject
import kotlinx.coroutines.flow.observeOn
import timber.log.Timber

@HiltViewModel
@Suppress("TooManyFunctions")
class LaoViewModel
@Inject
constructor(
    /*
     * Dependencies for this class
     */
    application: Application,
    private val laoRepo: LAORepository,
    private val rollCallRepo: RollCallRepository,
    private val witnessingRepo: WitnessingRepository,
    private val connectivityRepository: ConnectivityRepository,
    private val networkManager: GlobalNetworkManager,
    private val keyManager: KeyManager,
    private val wallet: Wallet,
    appDatabase: AppDatabase
) : AndroidViewModel(application), PopViewModel {

  override var laoId: String? = null

  var currentTab = MutableLiveData<MainMenuTab>()
    private set

  val isTab = MutableLiveData(java.lang.Boolean.TRUE)
  val pageTitle = MutableLiveData(0)

  var isOrganizer = false
  val isWitness = MutableLiveData(java.lang.Boolean.FALSE)
  val isAttendee = MutableLiveData(java.lang.Boolean.FALSE)
  val role = MutableLiveData(Role.MEMBER)

  val isInternetConnected = MutableLiveData(java.lang.Boolean.TRUE)

  private val disposables = CompositeDisposable()
  private val subscriptionsDao: SubscriptionsDao = appDatabase.subscriptionsDao()

  @get:Throws(UnknownLaoException::class)
  val lao: LaoView
    get() = laoRepo.getLaoView(laoId!!)

  fun setCurrentTab(tab: MainMenuTab) {
    currentTab.value = tab
  }

  private fun setIsWitness(isWitness: Boolean) {
    if (java.lang.Boolean.valueOf(isWitness) != this.isWitness.value) {
      this.isWitness.value = isWitness
    }
  }

  private fun setIsAttendee(isAttendee: Boolean) {
    if (java.lang.Boolean.valueOf(isAttendee) != this.isAttendee.value) {
      this.isAttendee.value = isAttendee
    }
  }

  fun setIsTab(isTab: Boolean) {
    if (java.lang.Boolean.valueOf(isTab) != this.isTab.value) {
      this.isTab.value = isTab
    }
  }

  override fun setPageTitle(@StringRes title: Int) {
    if (this.pageTitle.value != title) {
      this.pageTitle.value = title
    }
  }

  @Throws(KeyException::class, UnknownLaoException::class)
  fun getCurrentPopToken(rollCall: RollCall): PoPToken {
    return keyManager.getPoPToken(lao, rollCall)
  }

  fun getPublicKey(): PublicKey {
    return keyManager.mainPublicKey
  }

  fun updateRole() {
    val currentRole = determineRole()
    if (role.value !== currentRole) {
      role.value = currentRole
    }
  }

  /**
   * This function should be used to add disposable object generated from subscription to sent
   * messages flows
   *
   * They will be disposed of when the view model is cleaned which ensures that the subscription
   * stays relevant throughout the whole lifecycle of the activity and it is not bound to a fragment
   *
   * @param disposable to add
   */
  fun addDisposable(disposable: Disposable) {
    disposables.add(disposable)
  }

  override fun onCleared() {
    super.onCleared()
    disposables.dispose()
  }

  fun saveSubscriptionsData() {
    val toDispose = saveSubscriptionsRoutine(laoId!!, networkManager, subscriptionsDao)
    toDispose?.let { addDisposable(it) }
  }

  /** Function to restore the subscriptions to the given lao channels. */
  fun restoreConnections() {
    // Retrieve from the database the saved subscriptions
    val subscriptionsEntity = subscriptionsDao.getSubscriptionsByLao(laoId!!) ?: return
    if (subscriptionsEntity.serverAddress == networkManager.currentUrl &&
        (subscriptionsEntity.subscriptions == networkManager.messageSender.subscriptions)) {
      Timber.tag(TAG).d("Current connections are up to date")
      return
    }

    Timber.tag(TAG).d("Restoring connections")

    // Connect to the server and launch the connecting activity, as when joining a lao
    networkManager.connect(subscriptionsEntity.serverAddress, subscriptionsEntity.subscriptions)
    getApplication<Application>()
        .startActivity(
            newIntentForJoiningDetail(getApplication<Application>().applicationContext, laoId!!))
  }

  private fun determineRole(): Role {
    return when {
      isOrganizer -> {
        Role.ORGANIZER
      }
      java.lang.Boolean.TRUE == isWitness.value -> {
        Role.WITNESS
      }
      java.lang.Boolean.TRUE == isAttendee.value -> {
        Role.ATTENDEE
      }
      else -> {
        Role.MEMBER
      }
    }
  }

  fun observeLao(laoId: String) {
    addDisposable(
        laoRepo
            .getLaoObservable(laoId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { laoView: LaoView ->
                  Timber.tag(TAG).d("got an update for lao: %s", laoView)

                  isOrganizer = laoView.organizer == keyManager.mainPublicKey
                  setIsWitness(witnessingRepo.isWitness(laoId, keyManager.mainPublicKey))

                  updateRole()
                },
                { error: Throwable -> Timber.tag(TAG).e(error, "error updating LAO") }))
  }

  fun observeRollCalls(laoId: String) {
    addDisposable(
        rollCallRepo
            .getRollCallsObservableInLao(laoId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { rollCalls: Set<RollCall> ->
                  val isLastRollCallAttended =
                      rollCalls
                          .stream()
                          .filter { rc: RollCall -> isRollCallAttended(rc, laoId) }
                          .anyMatch { rc: RollCall ->
                            try {
                              return@anyMatch rc == rollCallRepo.getLastClosedRollCall(laoId)
                            } catch (e: NoRollCallException) {
                              Timber.tag(TAG).e(e)
                              return@anyMatch false
                            }
                          }

                  setIsAttendee(isLastRollCallAttended)
                },
                { error: Throwable ->
                  logAndShow(getApplication(), TAG, error, R.string.unknown_roll_call_exception)
                }))
  }

  fun observeInternetConnection() {
    addDisposable(
        connectivityRepository
            .observeConnectivity()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { isConnected -> isInternetConnected.value = isConnected },
                { error: Throwable -> Timber.tag(TAG).e(error, "error connection status") }))
  }

  private fun isRollCallAttended(rollcall: RollCall, laoId: String): Boolean {
    return try {
      val pk = wallet.generatePoPToken(laoId, rollcall.persistentId).publicKey
      rollcall.isClosed && rollcall.attendees.contains(pk)
    } catch (e: Exception) {
      when (e) {
        is KeyGenerationException,
        is UninitializedWalletException -> {
          Timber.tag(TAG).e(e, "failed to retrieve public key from wallet")
          false
        }
        else -> throw e
      }
    }
  }

  val isWitnessingEnabled: Boolean
    get() = !witnessingRepo.areWitnessesEmpty(laoId!!)

  companion object {
    val TAG: String = LaoViewModel::class.java.simpleName
  }
}
