package com.github.dedis.popstellar.ui.home;

import android.app.Activity;
import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.*;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.qrcode.ConnectToLao;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.local.PersistentData;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningViewModel;
import com.github.dedis.popstellar.ui.qrcode.ScanningAction;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.keys.SeedValidationException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.security.GeneralSecurityException;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.BackpressureStrategy;
import io.reactivex.disposables.CompositeDisposable;

@HiltViewModel
public class HomeViewModel extends AndroidViewModel implements QRCodeScanningViewModel {

  public static final String TAG = HomeViewModel.class.getSimpleName();

  private static final ScanningAction scanningAction = ScanningAction.ADD_LAO_PARTICIPANT;

  /** LiveData objects that represent the state in a fragment */
  private final MutableLiveData<Boolean> isWalletSetup = new MutableLiveData<>(false);

  private final MutableLiveData<HomeTab> currentTab = new MutableLiveData<>(HomeTab.HOME);
  private final LiveData<List<Lao>> laos;
  private final LiveData<Boolean> isSocialMediaEnabled;

  /** Dependencies for this class */
  private final Gson gson;

  private final KeyManager keyManager;
  private final Wallet wallet;
  private final GlobalNetworkManager networkManager;

  private final CompositeDisposable disposables = new CompositeDisposable();

  @Inject
  public HomeViewModel(
      @NonNull Application application,
      Gson gson,
      Wallet wallet,
      LAORepository laoRepository,
      KeyManager keyManager,
      GlobalNetworkManager networkManager) {
    super(application);

    this.gson = gson;
    this.keyManager = keyManager;
    this.wallet = wallet;
    this.networkManager = networkManager;

    laos =
        LiveDataReactiveStreams.fromPublisher(
            laoRepository.getAllLaos().toFlowable(BackpressureStrategy.BUFFER));
    isSocialMediaEnabled = Transformations.map(laos, laoSet -> laoSet != null && !laoSet.isEmpty());
  }

  @Override
  public int getScanDescription() {
    return R.string.qrcode_scanning_connect_lao;
  }

  @Override
  public ScanningAction getScanningAction() {
    return scanningAction;
  }

  @Override
  public boolean addManually(String data) {
    Log.d(TAG, "Lao data added manually with value: " + data);
    handleConnectionToLao(data);
    return true;
  }

  @Override
  public void onQRCodeDetected(Barcode barcode) {
    Log.d(TAG, "Detected barcode with value: " + barcode.rawValue);
    handleConnectionToLao(barcode.rawValue);
  }

  private void handleConnectionToLao(String data) {
    ConnectToLao laoData;
    try {
      laoData = ConnectToLao.extractFrom(gson, data);
    } catch (JsonParseException e) {
      Log.e(TAG, "Invalid QRCode laoData", e);
      Toast.makeText(
              getApplication().getApplicationContext(), "Invalid QRCode laoData", Toast.LENGTH_LONG)
          .show();
      return;
    }

    // Establish connection with new address
    networkManager.connect(laoData.server);
    getApplication()
        .startActivity(
            ConnectingActivity.newIntentForDetail(
                getApplication().getApplicationContext(), laoData.lao));
  }

  protected void restoreConnections(PersistentData data) {
    if (data == null) {
      return;
    }
    Log.d(TAG, "Saved state found : " + data);

    if (!isWalletSetUp()) {
      Log.d(TAG, "Restoring wallet");
      String appended = String.join(" ", data.getWalletSeed());
      try {
        importSeed(appended);
      } catch (GeneralSecurityException | SeedValidationException e) {
        Log.e(TAG, "error importing seed from memory");
        return;
      }
    }

    if (data.getSubscriptions().equals(networkManager.getMessageSender().getSubscriptions())) {
      Log.d(TAG, "current state is up to date");
      return;
    }
    Log.d(TAG, "restoring connections");
    networkManager.connect(data.getServerAddress(), data.getSubscriptions());
    getApplication()
        .startActivity(
            ConnectingActivity.newIntentForHome(getApplication().getApplicationContext()));
  }

  public void savePersistentData() {
    ActivityUtils.activitySavingRoutine(
        networkManager, wallet, getApplication().getApplicationContext());
  }

  /** onCleared is used to cancel all subscriptions to observables. */
  @Override
  protected void onCleared() {
    super.onCleared();

    disposables.dispose();
  }

  /**
   * launchLao is invoked when the user tries to create a new LAO. The method creates a `CreateLAO`
   * message and publishes it to the root channel. It observers the response in the background and
   * switches to the home screen on success.
   */
  public void launchLao(Activity activity, String laoName) {
    Log.d(TAG, "creating lao with name " + laoName);
    CreateLao createLao = new CreateLao(laoName, keyManager.getMainPublicKey());

    disposables.add(
        networkManager
            .getMessageSender()
            .publish(keyManager.getMainKeyPair(), Channel.ROOT, createLao)
            .subscribe(
                () -> {
                  Log.d(TAG, "got success result for create lao");
                  Lao lao = new Lao(createLao.getId());

                  // Send subscribe and catchup
                  networkManager
                      .getMessageSender()
                      .subscribe(lao.getChannel())
                      .subscribe(
                          () -> {
                            Log.d(TAG, "subscribing to LAO with id " + lao.getId());
                            activity.startActivity(
                                LaoDetailActivity.newIntentForLao(activity, lao.getId()));
                          },
                          error ->
                              ErrorUtils.logAndShow(
                                  getApplication(), TAG, error, R.string.error_create_lao));
                },
                error ->
                    ErrorUtils.logAndShow(
                        getApplication(), TAG, error, R.string.error_create_lao)));
  }

  public void importSeed(String seed) throws GeneralSecurityException, SeedValidationException {
    wallet.importSeed(seed);
    setIsWalletSetUp(true);
  }

  public void newSeed() {
    wallet.newSeed();
  }

  /** Getters for LiveData instances declared above */
  public LiveData<HomeTab> getCurrentTab() {
    return currentTab;
  }

  public LiveData<List<Lao>> getLAOs() {
    return laos;
  }

  public LiveData<Boolean> isSocialMediaEnabled() {
    return isSocialMediaEnabled;
  }

  public LiveData<Boolean> getIsWalletSetUpEvent() {
    return isWalletSetup;
  }

  public void setCurrentTab(HomeTab tab) {
    this.currentTab.postValue(tab);
  }

  public void setIsWalletSetUp(boolean isSetUp) {
    this.isWalletSetup.setValue(isSetUp);
  }

  public boolean isWalletSetUp() {
    Boolean setup = isWalletSetup.getValue();
    if (setup == null) return false;
    else return setup;
  }

  public void logoutWallet() {
    wallet.logout();
    setIsWalletSetUp(false);
  }
}
