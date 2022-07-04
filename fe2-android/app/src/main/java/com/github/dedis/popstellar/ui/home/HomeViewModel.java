package com.github.dedis.popstellar.ui.home;

import static androidx.core.content.ContextCompat.checkSelfPermission;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.SingleEvent;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.Wallet;
import com.github.dedis.popstellar.model.qrcode.ConnectToLao;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.qrcode.CameraPermissionViewModel;
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningViewModel;
import com.github.dedis.popstellar.ui.qrcode.ScanningAction;
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
public class HomeViewModel extends AndroidViewModel
    implements CameraPermissionViewModel, QRCodeScanningViewModel {

  public static final String TAG = HomeViewModel.class.getSimpleName();

  public enum HomeViewAction {
    SCAN,
    REQUEST_CAMERA_PERMISSION
  }

  private static final ScanningAction scanningAction = ScanningAction.ADD_LAO_PARTICIPANT;

  /** LiveData objects for capturing events like button clicks */
  private final MutableLiveData<SingleEvent<String>> mOpenLaoEvent = new MutableLiveData<>();

  private final MutableLiveData<SingleEvent<String>> mOpenConnectingEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenHomeEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<HomeViewAction>> mOpenConnectEvent =
      new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenLaunchEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mLaunchNewLaoEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mCancelNewLaoEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenWalletEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenSeedEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<String>> mOpenLaoWalletEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenSettingsEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenSocialMediaEvent =
      new MutableLiveData<>();

  /** LiveData objects that represent the state in a fragment */
  private final MutableLiveData<Boolean> mIsWalletSetUp = new MutableLiveData<>(false);

  private final MutableLiveData<String> mLaoName = new MutableLiveData<>();
  private final LiveData<List<Lao>> mLAOs;

  /** Dependencies for this class */
  private final Gson gson;

  private final LAORepository laoRepository;
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

    this.laoRepository = laoRepository;
    this.gson = gson;
    this.keyManager = keyManager;
    this.wallet = wallet;
    this.networkManager = networkManager;

    mLAOs =
        LiveDataReactiveStreams.fromPublisher(
            this.laoRepository.getAllLaos().toFlowable(BackpressureStrategy.BUFFER));
  }

  @Override
  public void onPermissionGranted() {
    openQrCodeScanning();
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

    openConnecting(laoData.lao);
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
  @SuppressLint("CheckResult")
  public void launchLao() {
    String laoName = mLaoName.getValue();

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
                            openLAO(lao.getId());
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
    openWallet();
  }

  public void newSeed() {
    wallet.newSeed();
    mOpenSeedEvent.postValue(new SingleEvent<>(true));
  }

  /** Getters for MutableLiveData instances declared above */
  public LiveData<List<Lao>> getLAOs() {
    return mLAOs;
  }

  public LiveData<SingleEvent<String>> getOpenLaoEvent() {
    return mOpenLaoEvent;
  }

  public LiveData<SingleEvent<Boolean>> getOpenHomeEvent() {
    return mOpenHomeEvent;
  }

  public LiveData<SingleEvent<String>> getOpenConnectingEvent() {
    return mOpenConnectingEvent;
  }

  public LiveData<SingleEvent<HomeViewAction>> getOpenConnectEvent() {
    return mOpenConnectEvent;
  }

  public LiveData<SingleEvent<Boolean>> getOpenLaunchEvent() {
    return mOpenLaunchEvent;
  }

  public LiveData<SingleEvent<Boolean>> getLaunchNewLaoEvent() {
    return mLaunchNewLaoEvent;
  }

  public LiveData<SingleEvent<Boolean>> getCancelNewLaoEvent() {
    return mCancelNewLaoEvent;
  }

  public Boolean isWalletSetUp() {
    return mIsWalletSetUp.getValue();
  }

  public LiveData<Boolean> getIsWalletSetUpEvent() {
    return mIsWalletSetUp;
  }

  public LiveData<SingleEvent<Boolean>> getOpenWalletEvent() {
    return mOpenWalletEvent;
  }

  public LiveData<SingleEvent<Boolean>> getOpenSeedEvent() {
    return mOpenSeedEvent;
  }

  public LiveData<SingleEvent<String>> getOpenLaoWalletEvent() {
    return mOpenLaoWalletEvent;
  }

  public LiveData<SingleEvent<Boolean>> getOpenSettingsEvent() {
    return mOpenSettingsEvent;
  }

  public LiveData<SingleEvent<Boolean>> getOpenSocialMediaEvent() {
    return mOpenSocialMediaEvent;
  }

  /*
   * Methods that modify the state or post an Event to update the UI.
   */

  public void openLAO(String laoId) {
    mOpenLaoEvent.postValue(new SingleEvent<>(laoId));
  }

  public void openHome() {
    mOpenHomeEvent.postValue(new SingleEvent<>(true));
  }

  public void openConnecting(String laoId) {
    mOpenConnectingEvent.postValue(new SingleEvent<>(laoId));
  }

  public void openWallet() {
    mOpenWalletEvent.postValue(new SingleEvent<>(isWalletSetUp()));
  }

  public void openLaoWallet(String laoId) {
    mOpenLaoWalletEvent.postValue(new SingleEvent<>(laoId));
  }

  public void openConnect() {
    if (checkSelfPermission(getApplication().getApplicationContext(), Manifest.permission.CAMERA)
        == PackageManager.PERMISSION_GRANTED) {
      openQrCodeScanning();
    } else {
      openCameraPermission();
    }
  }

  public void openQrCodeScanning() {
    mOpenConnectEvent.setValue(new SingleEvent<>(HomeViewAction.SCAN));
  }

  public void openCameraPermission() {
    mOpenConnectEvent.setValue(new SingleEvent<>(HomeViewAction.REQUEST_CAMERA_PERMISSION));
  }

  public void openLaunch() {
    mOpenLaunchEvent.setValue(new SingleEvent<>(true));
  }

  public void openSettings() {
    mOpenSettingsEvent.setValue(new SingleEvent<>(true));
  }

  public void openSocialMedia() {
    mOpenSocialMediaEvent.setValue(new SingleEvent<>(true));
  }

  public void launchNewLao() {
    mLaunchNewLaoEvent.setValue(new SingleEvent<>(true));
  }

  public void cancelNewLao() {
    mCancelNewLaoEvent.setValue(new SingleEvent<>(true));
  }

  public void setLaoName(String name) {
    this.mLaoName.setValue(name);
  }

  public void setIsWalletSetUp(Boolean isSetUp) {
    this.mIsWalletSetUp.setValue(isSetUp);
  }

  public void logoutWallet() {
    wallet.logout();
    setIsWalletSetUp(false);
    openWallet();
  }
}
