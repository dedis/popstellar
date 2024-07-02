package com.github.dedis.popstellar.ui.home

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.objects.Wallet
import com.github.dedis.popstellar.model.objects.view.LaoView
import com.github.dedis.popstellar.model.qrcode.ConnectToLao
import com.github.dedis.popstellar.model.qrcode.ConnectToLao.Companion.extractFrom
import com.github.dedis.popstellar.repository.ConnectivityRepository
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.database.AppDatabase
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager
import com.github.dedis.popstellar.ui.PopViewModel
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningViewModel
import com.github.dedis.popstellar.utility.ActivityUtils.saveWalletRoutine
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.error.keys.SeedValidationException
import com.google.gson.Gson
import com.google.gson.JsonParseException
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.security.GeneralSecurityException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class HomeViewModel
@Inject
/** Dependencies for this class */
constructor(
    application: Application,
    private val gson: Gson,
    private val wallet: Wallet,
    private val laoRepository: LAORepository,
    private val networkManager: GlobalNetworkManager,
    private val connectivityRepository: ConnectivityRepository,
    private val appDatabase: AppDatabase
) : AndroidViewModel(application), QRCodeScanningViewModel, PopViewModel {
  /** LiveData objects that represent the state in a fragment */
  private val isWalletSetup = MutableLiveData(false)

  val laoIdList: LiveData<List<String>> =
      LiveDataReactiveStreams.fromPublisher(
          laoRepository.allLaoIds.toFlowable(BackpressureStrategy.BUFFER))

  private val mPageTitle = MutableLiveData<Int>()

  /** This LiveData boolean is used to indicate whether the HomeFragment is displayed */
  val isHome = MutableLiveData(java.lang.Boolean.TRUE)

  val isInternetConnected = MutableLiveData(java.lang.Boolean.TRUE)

  val isWitnessingEnabled = MutableLiveData(java.lang.Boolean.FALSE)

  /**
   * This atomic flag is used to avoid the scanner fragment to open multiple connecting activities,
   * as in few seconds it scans the same qr code multiple times. This flag signals when a connecting
   * attempt finishes, so that a new one could be launched.
   */
  private val connecting = AtomicBoolean(false)
  private val disposables = CompositeDisposable()

  override fun onCleared() {
    super.onCleared()
    disposables.dispose()
  }

  override val nbScanned: LiveData<Int?>
    get() = // This is useless for the HomeActivity and must be implemented for the scanner
    MutableLiveData(0)

  override val laoId: String?
    get() = // This is useless for the HomeActivity and must be implemented for the scanner
    null

  override fun handleData(data: String?) {
    // Don't process another data from the scanner if I'm already trying to connect on a qr code
    if (connecting.get()) {
      return
    }

    val laoData: ConnectToLao =
        try {
          extractFrom(gson, data)
        } catch (e: JsonParseException) {
          Timber.tag(TAG).e(e, "Invalid QRCode laoData")
          Toast.makeText(
                  getApplication<Application>().applicationContext,
                  R.string.invalid_qrcode_lao_data,
                  Toast.LENGTH_LONG)
              .show()
          return
        }

    // Establish connection with new address
    networkManager.connect(laoData.server)
    connecting.set(true)

    getApplication<Application>()
        .startActivity(
            ConnectingActivity.newIntentForJoiningDetail(
                getApplication<Application>().applicationContext, laoData.lao))
  }

  /**
   * Function to restore the wallet of the application.
   *
   * @return true if the wallet is correctly restored, false otherwise
   */
  fun restoreWallet(): Boolean {
    // Retrieve from the database the saved wallet
    val walletEntity = appDatabase.walletDao().wallet
    if (walletEntity == null) {
      logAndShow(
          getApplication<Application>().applicationContext, TAG, R.string.no_seed_storage_found)
      return false
    }

    if (!isWalletSetUp) {
      // Restore the wallet if not already set up
      val seed = walletEntity.walletSeedArray
      val appended = seed.joinToString(" ")
      Timber.tag(TAG).d("Retrieved wallet from db, seed : %s", appended)
      try {
        importSeed(appended)
      } catch (e: Exception) {
        when (e) {
          is GeneralSecurityException,
          is SeedValidationException -> {
            Timber.tag(TAG).e(e, "Error importing seed from storage")
            return false
          }
          else -> throw e
        }
      }
    }

    return true
  }

  @Throws(GeneralSecurityException::class)
  fun saveWallet() {
    addDisposable(saveWalletRoutine(wallet, appDatabase.walletDao()))
  }

  fun clearStorage() {
    Executors.newCachedThreadPool().execute {
      appDatabase.clearAllTables()
      Timber.tag(TAG).d("All the tables in the database have been cleared")
    }
    networkManager.dispose()
    laoRepository.clearRepository()
  }

  @Throws(GeneralSecurityException::class, SeedValidationException::class)
  fun importSeed(seed: String) {
    wallet.importSeed(seed)
    isWalletSetUp = true
  }

  @Throws(UnknownLaoException::class)
  fun getLaoView(laoId: String): LaoView {
    return laoRepository.getLaoView(laoId)
  }

  val isWalletSetUpEvent: LiveData<Boolean>
    get() = isWalletSetup

  val pageTitle: LiveData<Int>
    get() = mPageTitle

  override fun setPageTitle(title: Int) {
    mPageTitle.postValue(title)
  }

  /**
   * Function to set the liveData isHome.
   *
   * @param isHome true if the current fragment is HomeFragment, false otherwise
   */
  fun setIsHome(isHome: Boolean) {
    if (java.lang.Boolean.valueOf(isHome) != this.isHome.value) {
      this.isHome.value = isHome
    }
  }

  fun observeInternetConnection() {
    addDisposable(
        connectivityRepository
            .observeConnectivity()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { isConnected -> isInternetConnected.value = isConnected },
                { error: Throwable ->
                  Timber.tag(LaoViewModel.TAG).e(error, "error connection status")
                }))
  }

  /**
   * Function to set the liveData isWitnessingEnabled.
   *
   * @param isWitnessingEnabled true if we want to enable witnessing, false otherwise
   */
  fun setIsWitnessingEnabled(isWitnessingEnabled: Boolean) {
    if (java.lang.Boolean.valueOf(isWitnessingEnabled) != this.isWitnessingEnabled.value) {
      this.isWitnessingEnabled.value = isWitnessingEnabled
    }
  }

  /** Set to false the connecting flag when the connecting activity has finished */
  fun disableConnectingFlag() {
    connecting.set(false)
  }

  var isWalletSetUp: Boolean
    get() {
      val setup = isWalletSetup.value
      return setup ?: false
    }
    set(isSetUp) {
      isWalletSetup.value = isSetUp
    }

  fun logoutWallet() {
    wallet.logout()
    isWalletSetUp = false
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

  companion object {
    val TAG: String = HomeViewModel::class.java.simpleName
  }
}
