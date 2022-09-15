package com.github.dedis.popstellar.ui.home;

import android.app.Application;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.*;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.model.qrcode.ConnectToLao;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.local.PersistentData;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.navigation.NavigationViewModel;
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningViewModel;
import com.github.dedis.popstellar.ui.qrcode.ScanningAction;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.Constants;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
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
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

@HiltViewModel
public class HomeViewModel extends NavigationViewModel<HomeTab> implements QRCodeScanningViewModel {

  public static final String TAG = HomeViewModel.class.getSimpleName();

  private static final ScanningAction scanningAction = ScanningAction.ADD_LAO_PARTICIPANT;

  /** LiveData objects that represent the state in a fragment */
  private final MutableLiveData<Boolean> isWalletSetup = new MutableLiveData<>(false);

  private final LiveData<List<String>> laoIdList;
  private final LiveData<Boolean> isSocialMediaEnabled;

  /** Dependencies for this class */
  private final Gson gson;

  private final KeyManager keyManager;
  private final Wallet wallet;
  private final GlobalNetworkManager networkManager;
  private final LAORepository laoRepository;

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
    this.laoRepository = laoRepository;

    laoIdList =
        LiveDataReactiveStreams.fromPublisher(
            laoRepository.getAllLaoIds().toFlowable(BackpressureStrategy.BUFFER));

    isSocialMediaEnabled =
        Transformations.map(laoIdList, laoSet -> laoSet != null && !laoSet.isEmpty());
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    disposables.dispose();
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
        Log.e(TAG, "error importing seed from storage");
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

  public void savePersistentData() throws GeneralSecurityException {
    ActivityUtils.activitySavingRoutine(
        networkManager, wallet, getApplication().getApplicationContext());
  }

  /**
   * launchLao is invoked when the user tries to create a new LAO. The method creates a `CreateLAO`
   * message and publishes it to the root channel. It observers the response in the background and
   * switches to the home screen on success.
   *
   * @return a single containing the id of the launched lao if it was successful
   */
  public Single<String> launchLao(String laoName) {
    Log.d(TAG, "creating lao with name " + laoName);
    CreateLao createLao = new CreateLao(laoName, keyManager.getMainPublicKey());
    Lao lao = new Lao(createLao.getId());

    return networkManager
        .getMessageSender()
        .publish(keyManager.getMainKeyPair(), Channel.ROOT, createLao)
        .doOnComplete(() -> Log.d(TAG, "got success result for create lao with id " + lao.getId()))
        .andThen(networkManager.getMessageSender().subscribe(lao.getChannel()))
        .toSingleDefault(lao.getId());
  }

  public void importSeed(String seed) throws GeneralSecurityException, SeedValidationException {
    wallet.importSeed(seed);
    setIsWalletSetUp(true);
  }

  public void newSeed() throws GeneralSecurityException {
    wallet.newSeed();
  }

  public LiveData<List<String>> getLaoIdList() {
    return laoIdList;
  }

  public LaoView getLaoView(String laoId) throws UnknownLaoException {
    return laoRepository.getLaoView(laoId);
  }

  public LiveData<Boolean> isSocialMediaEnabled() {
    return isSocialMediaEnabled;
  }

  public LiveData<Boolean> getIsWalletSetUpEvent() {
    return isWalletSetup;
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

  public void openConnecting(String laoId) {
    Intent intent = new Intent(getApplication().getApplicationContext(), ConnectingActivity.class);
    intent.putExtra(Constants.LAO_ID_EXTRA, laoId);
    intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
    getApplication().startActivity(intent);
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
