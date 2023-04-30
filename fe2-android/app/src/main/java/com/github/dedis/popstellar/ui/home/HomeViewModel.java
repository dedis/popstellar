package com.github.dedis.popstellar.ui.home;

import android.app.Application;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import androidx.room.EmptyResultSetException;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.Wallet;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.model.qrcode.ConnectToLao;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.repository.database.core.CoreEntity;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.PopViewModel;
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningViewModel;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.error.keys.SeedValidationException;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@HiltViewModel
public class HomeViewModel extends AndroidViewModel
    implements QRCodeScanningViewModel, PopViewModel {

  public static final String TAG = HomeViewModel.class.getSimpleName();

  /** LiveData objects that represent the state in a fragment */
  private final MutableLiveData<Boolean> isWalletSetup = new MutableLiveData<>(false);

  private final LiveData<List<String>> laoIdList;
  private final MutableLiveData<Integer> mPageTitle = new MutableLiveData<>();

  /** This LiveData boolean is used to indicate whether the HomeFragment is displayed */
  private final MutableLiveData<Boolean> isHome = new MutableLiveData<>(Boolean.TRUE);

  /** Dependencies for this class */
  private final Gson gson;

  private final Wallet wallet;
  private final GlobalNetworkManager networkManager;
  private final LAORepository laoRepository;

  private final AppDatabase appDatabase;

  private final CompositeDisposable disposables = new CompositeDisposable();

  @Inject
  public HomeViewModel(
      @NonNull Application application,
      Gson gson,
      Wallet wallet,
      LAORepository laoRepository,
      GlobalNetworkManager networkManager,
      AppDatabase appDatabase) {
    super(application);

    this.gson = gson;
    this.wallet = wallet;
    this.networkManager = networkManager;
    this.laoRepository = laoRepository;
    this.appDatabase = appDatabase;

    laoIdList =
        LiveDataReactiveStreams.fromPublisher(
            laoRepository.getAllLaoIds().toFlowable(BackpressureStrategy.BUFFER));
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    disposables.dispose();
  }

  @Override
  public LiveData<Integer> getNbScanned() {
    // This is useless for the HomeActivity and must be implemented for the scanner
    return new MutableLiveData<>(0);
  }

  @Override
  public String getLaoId() {
    // This is useless for the HomeActivity and must be implemented for the scanner
    return null;
  }

  @Override
  public void handleData(String data) {
    ConnectToLao laoData;
    try {
      laoData = ConnectToLao.extractFrom(gson, data);
    } catch (JsonParseException e) {
      Timber.tag(TAG).e(e, "Invalid QRCode laoData");
      Toast.makeText(
              getApplication().getApplicationContext(),
              R.string.invalid_qrcode_data,
              Toast.LENGTH_LONG)
          .show();
      return;
    }

    // Establish connection with new address
    networkManager.connect(laoData.server);
    getApplication()
        .startActivity(
            ConnectingActivity.newIntentForJoiningDetail(
                getApplication().getApplicationContext(), laoData.lao));
  }

  protected Single<Boolean> restoreConnections(Context context) {
    return appDatabase
        .coreDao()
        .getSettings()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .onErrorResumeNext(
            err -> {
              if (err instanceof EmptyResultSetException) {
                return Single.just(CoreEntity.getEmptyEntity());
              } else {
                return Single.error(err);
              }
            })
        .flatMap(
            entity -> {
              if (entity.equals(CoreEntity.getEmptyEntity())) {
                ErrorUtils.logAndShow(context, TAG, R.string.nothing_stored);
                return Single.just(false);
              }

              Timber.tag(TAG).d("Saved state found : %s", entity);

              if (!isWalletSetUp()) {
                Timber.tag(TAG).d("Restoring wallet");
                String[] seed = entity.getWalletSeedArray();
                if (seed.length == 0) {
                  ErrorUtils.logAndShow(
                      getApplication().getApplicationContext(),
                      TAG,
                      R.string.no_seed_storage_found);
                  return Single.just(false);
                }
                String appended = String.join(" ", seed);
                Timber.tag(TAG).d("Seed is %s", appended);
                try {
                  importSeed(appended);
                } catch (GeneralSecurityException | SeedValidationException e) {
                  Timber.tag(TAG).e(e, "Error importing seed from storage");
                  return Single.just(false);
                }
              }

              if (entity
                  .getSubscriptions()
                  .equals(networkManager.getMessageSender().getSubscriptions())) {
                Timber.tag(TAG).d("current state is up to date");
                return Single.just(true);
              }

              Timber.tag(TAG).d("restoring connections");
              networkManager.connect(entity.getServerAddress(), entity.getSubscriptionsCopy());
              getApplication()
                  .startActivity(
                      ConnectingActivity.newIntentForHome(
                          getApplication().getApplicationContext()));
              return Single.just(true);
            });
  }

  public void saveCoreData() throws GeneralSecurityException {
    ActivityUtils.activitySavingRoutine(networkManager, wallet, appDatabase.coreDao());
  }

  public void clearStorage() {
    laoRepository.clearRepository();
    Timber.tag(TAG).d("Clearing all databases in a background thread");
    Executors.newCachedThreadPool().execute(appDatabase::clearAllTables);
  }

  public void importSeed(String seed) throws GeneralSecurityException, SeedValidationException {
    wallet.importSeed(seed);
    setIsWalletSetUp(true);
  }

  public LiveData<List<String>> getLaoIdList() {
    return laoIdList;
  }

  public LaoView getLaoView(String laoId) throws UnknownLaoException {
    return laoRepository.getLaoView(laoId);
  }

  public LiveData<Boolean> getIsWalletSetUpEvent() {
    return isWalletSetup;
  }

  public void setIsWalletSetUp(boolean isSetUp) {
    this.isWalletSetup.setValue(isSetUp);
  }

  public LiveData<Integer> getPageTitle() {
    return mPageTitle;
  }

  @Override
  public void setPageTitle(int titleId) {
    mPageTitle.postValue(titleId);
  }

  public MutableLiveData<Boolean> isHome() {
    return isHome;
  }

  /**
   * Function to set the liveData isHome.
   *
   * @param isHome true if the current fragment is HomeFragment, false otherwise
   */
  public void setIsHome(boolean isHome) {
    if (!Boolean.valueOf(isHome).equals(this.isHome.getValue())) {
      this.isHome.setValue(isHome);
    }
  }

  public boolean isWalletSetUp() {
    Boolean setup = isWalletSetup.getValue();
    if (setup == null) {
      return false;
    } else {
      return setup;
    }
  }

  public void logoutWallet() {
    wallet.logout();
    setIsWalletSetUp(false);
  }

  /**
   * This function should be used to add disposable object generated from subscription to sent
   * messages flows
   *
   * <p>They will be disposed of when the view model is cleaned which ensures that the subscription
   * stays relevant throughout the whole lifecycle of the activity and it is not bound to a fragment
   *
   * @param disposable to add
   */
  public void addDisposable(Disposable disposable) {
    this.disposables.add(disposable);
  }
}
