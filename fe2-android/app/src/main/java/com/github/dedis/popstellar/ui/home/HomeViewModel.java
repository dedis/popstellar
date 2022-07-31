package com.github.dedis.popstellar.ui.home;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.*;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.qrcode.ConnectToLao;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningViewModel;
import com.github.dedis.popstellar.ui.qrcode.ScanningAction;
import com.github.dedis.popstellar.utility.Constants;
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

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

@HiltViewModel
public class HomeViewModel extends AndroidViewModel implements QRCodeScanningViewModel {

  public static final String TAG = HomeViewModel.class.getSimpleName();

  public enum HomeViewAction {
    SCAN,
    REQUEST_CAMERA_PERMISSION
  }

  private static final ScanningAction scanningAction = ScanningAction.ADD_LAO_PARTICIPANT;

  /** LiveData objects that represent the state in a fragment */
  private final MutableLiveData<Boolean> mIsWalletSetUp = new MutableLiveData<>(false);

  private final MutableLiveData<String> mLaoName = new MutableLiveData<>();
  private final LiveData<List<Lao>> mLAOs;

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

    mLAOs =
        LiveDataReactiveStreams.fromPublisher(
            laoRepository.getAllLaos().toFlowable(BackpressureStrategy.BUFFER));
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
  public void launchLao(Activity activity) {
    String laoName = mLaoName.getValue();

    Log.d(TAG, "creating lao with name " + laoName);
    CreateLao createLao = new CreateLao(laoName, keyManager.getMainPublicKey());
    Lao lao = new Lao(createLao.getId());

    disposables.add(
        networkManager
            .getMessageSender()
            .publish(keyManager.getMainKeyPair(), Channel.ROOT, createLao)
            .doOnComplete(
                () -> Log.d(TAG, "got success result for create lao with id " + lao.getId()))
            .toObservable()
            .flatMapCompletable(a -> networkManager.getMessageSender().subscribe(lao.getChannel()))
            .subscribe(
                () -> {
                  String laoId = lao.getId();
                  Log.d(TAG, "Opening lao detail activity on the home tab for lao " + laoId);
                  activity.startActivity(LaoDetailActivity.newIntentForLao(activity, laoId));
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

  public LiveData<List<Lao>> getLAOs() {
    return mLAOs;
  }

  public LiveData<Boolean> getIsWalletSetUpEvent() {
    return mIsWalletSetUp;
  }

  public void setLaoName(String name) {
    this.mLaoName.setValue(name);
  }

  public void setIsWalletSetUp(boolean isSetUp) {
    this.mIsWalletSetUp.setValue(isSetUp);
  }

  public boolean isWalletSetUp() {
    Boolean setup = mIsWalletSetUp.getValue();
    if (setup == null) return false;
    else return setup;
  }

  public void logoutWallet() {
    wallet.logout();
    setIsWalletSetUp(false);
  }

  public void openConnecting(String laoId) {
    Intent intent = new Intent(getApplication().getApplicationContext(), ConnectingActivity.class);
    intent.putExtra(Constants.LAO_ID_EXTRA, laoId);
    intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
    getApplication().startActivity(intent);
  }
}
