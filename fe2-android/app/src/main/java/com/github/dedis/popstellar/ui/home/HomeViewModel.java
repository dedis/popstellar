package com.github.dedis.popstellar.ui.home;

import android.Manifest;
import android.app.Application;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;
import com.github.dedis.popstellar.SingleEvent;
import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.Wallet;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.model.network.answer.Error;
import com.github.dedis.popstellar.model.network.answer.Result;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.ui.qrcode.CameraPermissionViewModel;
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningViewModel;
import com.github.dedis.popstellar.ui.qrcode.ScanningAction;
import com.github.dedis.popstellar.utility.security.Keys;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.PublicKeySign;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.gson.Gson;
import io.reactivex.BackpressureStrategy;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class HomeViewModel extends AndroidViewModel
    implements CameraPermissionViewModel, QRCodeScanningViewModel {

  public static final String TAG = HomeViewModel.class.getSimpleName();
  public static final String SCAN = "SCAN";
  public static final String REQUEST_CAMERA_PERMISSION = "REQUEST_CAMERA_PERMISSION";
  private static final ScanningAction scanningAction = ScanningAction.ADD_LAO_PARTICIPANT;

  /*
   * LiveData objects for capturing events like button clicks
   */
  private final MutableLiveData<SingleEvent<String>> mOpenLaoEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenHomeEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenConnectingEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<String>> mOpenConnectEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenLaunchEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mLaunchNewLaoEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mCancelNewLaoEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mCancelConnectEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenWalletEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenSeedEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<String>> mOpenLaoWalletEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mOpenSocialMediaEvent = new MutableLiveData<>();
  private final MutableLiveData<SingleEvent<Boolean>> mSendNewChirpEvent = new MutableLiveData<>();


  /*
   * LiveData objects that represent the state in a fragment
   */
  private final MutableLiveData<String> mConnectingLao = new MutableLiveData<>();
  private final MutableLiveData<Boolean> mIsWalletSetUp = new MutableLiveData<>(false);
  private final MutableLiveData<String> mLaoName = new MutableLiveData<>();
  private LiveData<List<Lao>> mLAOs;

  /*
   * Dependencies for this class
   */
  private final Gson mGson;
  private final LAORepository mLAORepository;
  private final AndroidKeysetManager mKeysetManager;
  private Wallet wallet;

  private Disposable disposable;

  public HomeViewModel(
      @NonNull Application application,
      Gson gson,
      LAORepository laoRepository,
      AndroidKeysetManager keysetManager) {
    super(application);

    mLAORepository = laoRepository;
    mGson = gson;
    mKeysetManager = keysetManager;
    wallet = Wallet.getInstance();

    mLAOs =
        LiveDataReactiveStreams.fromPublisher(
            mLAORepository.getAllLaos().toFlowable(BackpressureStrategy.BUFFER));


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
  public void onQRCodeDetected(Barcode barcode) {
    Log.d(TAG, "Detected barcode with value: " + barcode.rawValue);
    String channel = "/root/" + barcode.rawValue;
    mLAORepository
        .sendSubscribe(channel)
        .observeOn(AndroidSchedulers.mainThread())
        .timeout(3, TimeUnit.SECONDS)
        .subscribe(
            answer -> {
              if (answer instanceof Result) {
                Log.d(TAG, "got success result for subscribe to lao");
              } else {
                Log.d(
                    TAG,
                    "got failure result for subscribe to lao: "
                        + ((Error) answer).getError().getDescription());
              }
              openHome();
            },
            throwable -> {
              Log.d(TAG, "timed out waiting for a response for subscribe to lao", throwable);
              openHome(); //so that it doesn't load forever
            });
    setConnectingLao(channel);
    openConnecting();
  }

  /**
   * onCleared is used to cancel all subscriptions to observables.
   */
  @Override
  protected void onCleared() {
    if (disposable != null) {
      disposable.dispose();
    }
    super.onCleared();
  }

  /**
   * launchLao is invoked when the user tries to create a new LAO. The method creates a `CreateLAO`
   * message and publishes it to the root channel. It observers the response in the background and
   * switches to the home screen on success.
   */
  public void launchLao() {
    String laoName = mLaoName.getValue();

    try {
      KeysetHandle myKey = mKeysetManager.getKeysetHandle().getPublicKeysetHandle();
      String organizer = Keys.getEncodedKey(myKey);
      byte[] organizerBuf = Base64.getUrlDecoder().decode(organizer);

      Log.d(TAG, "creating lao with name " + laoName);
      CreateLao createLao = new CreateLao(laoName, organizer);
      PublicKeySign signer = mKeysetManager.getKeysetHandle().getPrimitive(PublicKeySign.class);

      MessageGeneral msg = new MessageGeneral(organizerBuf, createLao, signer, mGson);

      mLAORepository
          .sendPublish("/root", msg)
          .observeOn(AndroidSchedulers.mainThread())
          .timeout(5, TimeUnit.SECONDS)
          .subscribe(
              answer -> {
                if (answer instanceof Result) {
                  Log.d(TAG, "got success result for create lao");
                  openHome();
                } else {
                  Log.d(
                      TAG,
                      "got failure result for create lao: "
                          + ((Error) answer).getError().getDescription());
                }
              },
              throwable -> {
                Log.d(TAG, "timed out waiting for a response for create lao", throwable);
              });

    } catch (GeneralSecurityException e) {
      Log.d(TAG, "failed to get public key", e);
    } catch (IOException e) {
      Log.d(TAG, "failed to encode public key", e);
    }
  }

  public boolean importSeed(String seed) {
    try {
      if (wallet.importSeed(seed, new HashMap<>()) == null) {
        return false;
      } else {
        setIsWalletSetUp(true);
        openWallet();
        return true;
      }
    } catch (Exception e) {
      return false;
    }
  }

  /*
   * Getters for MutableLiveData instances declared above
   */
  public LiveData<List<Lao>> getLAOs() {
    return mLAOs;
  }

  public LiveData<SingleEvent<String>> getOpenLaoEvent() {
    return mOpenLaoEvent;
  }

  public LiveData<SingleEvent<Boolean>> getOpenHomeEvent() {
    return mOpenHomeEvent;
  }

  public LiveData<SingleEvent<Boolean>> getOpenConnectingEvent() {
    return mOpenConnectingEvent;
  }

  public LiveData<SingleEvent<String>> getOpenConnectEvent() {
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

  public LiveData<SingleEvent<Boolean>> getCancelConnectEvent() {
    return mCancelConnectEvent;
  }

  public LiveData<String> getConnectingLao() {
    return mConnectingLao;
  }

  public LiveData<String> getLaoName() {
    return mLaoName;
  }

  public Boolean isWalletSetUp() {
    return mIsWalletSetUp.getValue();
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

  public LiveData<SingleEvent<Boolean>> getOpenSocialMediaEvent() {
    return mOpenSocialMediaEvent;
  }

  public LiveData<SingleEvent<Boolean>> getSendNewChirpEvent() {
    return mSendNewChirpEvent;
  }


  /*
   * Methods that modify the state or post an Event to update the UI.
   */

  public void openLAO(String laoId) {
    mOpenLaoEvent.setValue(new SingleEvent<>(laoId));
  }

  public void openHome() {
    mOpenHomeEvent.postValue(new SingleEvent<>(true));
  }

  public void openConnecting() {
    mOpenConnectingEvent.postValue(new SingleEvent<>(true));
  }

  public void openWallet() {
    mOpenWalletEvent.postValue(new SingleEvent<>(isWalletSetUp()));
  }

  public void openSocialMedia() {
    mOpenSocialMediaEvent.postValue(new SingleEvent<>(true));
  }

  public void openSeed() {
    mOpenSeedEvent.postValue(new SingleEvent<>(true));
  }

  public void openLaoWallet(String laoId) {
    mOpenLaoWalletEvent.postValue(new SingleEvent<>(laoId));
  }

  public void openConnect() {
    if (ActivityCompat.checkSelfPermission(
        getApplication().getApplicationContext(), Manifest.permission.CAMERA)
        == PackageManager.PERMISSION_GRANTED) {
      openQrCodeScanning();
    } else {
      openCameraPermission();
    }
  }

  public void openQrCodeScanning() {
    mOpenConnectEvent.setValue(new SingleEvent<>(SCAN));
  }

  public void openCameraPermission() {
    mOpenConnectEvent.setValue(new SingleEvent<>(REQUEST_CAMERA_PERMISSION));
  }

  public void openLaunch() {
    mOpenLaunchEvent.setValue(new SingleEvent<>(true));
  }

  public void launchNewLao() {
    mLaunchNewLaoEvent.setValue(new SingleEvent<>(true));
  }

  public void cancelNewLao() {
    mCancelNewLaoEvent.setValue(new SingleEvent<>(true));
  }

  public void sendNewChirp() {
    mSendNewChirpEvent.setValue(new SingleEvent<>(true));
  }

  public void cancelConnect() {
    mCancelConnectEvent.setValue(new SingleEvent<>(true));
  }

  public void setConnectingLao(String lao) {
    this.mConnectingLao.postValue(lao);
  }

  public void setLaoName(String name) {
    this.mLaoName.setValue(name);
  }

  public void setIsWalletSetUp(Boolean isSetUp) {
    this.mIsWalletSetUp.setValue(isSetUp);
  }

  public void logoutWallet() {
    Wallet.getInstance().logout();
    setIsWalletSetUp(false);
    openWallet();
  }
}
