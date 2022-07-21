package com.github.dedis.popstellar.ui.home;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultRegistry;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.*;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.qrcode.ConnectToLao;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.home.connecting.ConnectingActivity;
import com.github.dedis.popstellar.ui.qrcode.*;
import com.github.dedis.popstellar.ui.socialmedia.SocialMediaActivity;
import com.github.dedis.popstellar.ui.wallet.*;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.Constants;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.keys.SeedValidationException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.function.Supplier;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.BackpressureStrategy;
import io.reactivex.disposables.CompositeDisposable;

import static com.github.dedis.popstellar.ui.socialmedia.SocialMediaActivity.OPENED_FROM;

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
                            String laoId = lao.getId();
                            Intent intent = new Intent(getApplication(), LaoDetailActivity.class);
                            Log.d(TAG, "Trying to open lao detail for lao with id " + laoId);
                            intent.putExtra(Constants.LAO_ID_EXTRA, laoId);
                            intent.putExtra(
                                Constants.FRAGMENT_TO_OPEN_EXTRA, Constants.LAO_DETAIL_EXTRA);
                            getApplication().startActivity(intent);
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

  /** Getters for MutableLiveData instances declared above */
  public LiveData<List<Lao>> getLAOs() {
    return mLAOs;
  }

  public Boolean isWalletSetUp() {
    return mIsWalletSetUp.getValue();
  }

  public LiveData<Boolean> getIsWalletSetUpEvent() {
    return mIsWalletSetUp;
  }

  public void openHome(FragmentManager manager) {
    setCurrentFragment(manager, R.id.fragment_home, HomeFragment::newInstance);
  }

  public void openCameraPermission(FragmentManager manager, ActivityResultRegistry registry) {
    setCurrentFragment(
        manager, R.id.fragment_camera_perm, () -> CameraPermissionFragment.newInstance(registry));
  }

  public void openLaunch(FragmentManager manager) {
    setCurrentFragment(manager, R.id.fragment_launch, LaunchFragment::newInstance);
  }

  public void openConnecting(String laoId) {
    Intent intent = new Intent(getApplication().getApplicationContext(), ConnectingActivity.class);
    intent.putExtra(Constants.LAO_ID_EXTRA, laoId);
    getApplication().startActivity(intent);
  }

  public void openQrCodeScanning(FragmentManager manager) {
    setCurrentFragment(manager, R.id.fragment_qrcode, QRCodeScanningFragment::new);
  }

  public void openSeedWallet(FragmentManager manager) {
    setCurrentFragment(manager, R.id.fragment_seed_wallet, SeedWalletFragment::new);
  }

  public void openWallet(FragmentManager manager) {
    if (isWalletSetUp()) {
      setCurrentFragment(manager, R.id.fragment_content_wallet, ContentWalletFragment::newInstance);
    } else {
      setCurrentFragment(manager, R.id.fragment_wallet, WalletFragment::newInstance);
    }
  }

  public void openLao(String laoId) {
    Intent intent = new Intent(getApplication(), LaoDetailActivity.class);
    Log.d(TAG, "Trying to open lao detail for lao with id " + laoId);
    intent.putExtra(Constants.LAO_ID_EXTRA, laoId);
    intent.putExtra(Constants.FRAGMENT_TO_OPEN_EXTRA, Constants.LAO_DETAIL_EXTRA);
    getApplication().startActivity(intent);
  }

  public void openLaoWallet(String laoId) {
    Intent intent = new Intent(getApplication(), LaoDetailActivity.class);
    Log.d(TAG, "Trying to open lao detail for lao with id " + laoId);
    intent.putExtra(Constants.LAO_ID_EXTRA, laoId);
    intent.putExtra(Constants.FRAGMENT_TO_OPEN_EXTRA, Constants.CONTENT_WALLET_EXTRA);
    getApplication().startActivity(intent);
  }

  public void openSocialMedia() {
    Intent intent = new Intent(getApplication(), SocialMediaActivity.class);
    Log.d(HomeViewModel.TAG, "Trying to open social media");
    intent.putExtra(OPENED_FROM, HomeViewModel.TAG);
    getApplication().startActivity(intent);
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
  }

  /**
   * Set the current fragment in the container of the activity
   *
   * @param manager of the fragments
   * @param id of the fragment
   * @param fragmentSupplier provides the fragment if it is missing
   */
  public static void setCurrentFragment(
      FragmentManager manager, @IdRes int id, Supplier<Fragment> fragmentSupplier) {
    Fragment fragment = manager.findFragmentById(id);
    // If the fragment was not created yet, create it now
    if (fragment == null) fragment = fragmentSupplier.get();

    // Set the new fragment in the container
    ActivityUtils.replaceFragmentInActivity(manager, fragment, R.id.fragment_container_home);
  }
}
