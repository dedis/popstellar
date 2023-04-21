package com.github.dedis.popstellar.ui.home;

import android.app.Application;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.*;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.Wallet;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.model.qrcode.ConnectToLao;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.local.PersistentData;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.PopViewModel;
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningViewModel;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.error.keys.SeedValidationException;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.BackpressureStrategy;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

@HiltViewModel
public class HomeViewModel extends AndroidViewModel
    implements QRCodeScanningViewModel, PopViewModel {

  private static final Logger logger = LogManager.getLogger(HomeViewModel.class);

  /** LiveData objects that represent the state in a fragment */
  private final MutableLiveData<Boolean> isWalletSetup = new MutableLiveData<>(false);

  private final LiveData<List<String>> laoIdList;
  private final MutableLiveData<Integer> mPageTitle = new MutableLiveData<>();
  private final MutableLiveData<Boolean> isHome = new MutableLiveData<>(Boolean.TRUE);

  /** Dependencies for this class */
  private final Gson gson;

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
      GlobalNetworkManager networkManager) {
    super(application);

    this.gson = gson;
    this.wallet = wallet;
    this.networkManager = networkManager;
    this.laoRepository = laoRepository;

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
      logger.error("Invalid QRCode laoData", e);
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

  protected void restoreConnections(PersistentData data) {
    if (data == null) {
      return;
    }
    logger.debug("Saved state found : " + data);

    if (!isWalletSetUp()) {
      logger.debug("Restoring wallet");
      String[] seed = data.getWalletSeed();
      logger.debug("seed is " + Arrays.toString(seed));
      if (seed.length == 0) {
        ErrorUtils.logAndShow(
            getApplication().getApplicationContext(), logger, R.string.no_seed_storage_found);
        return;
      }
      String appended = String.join(" ", data.getWalletSeed());
      try {
        importSeed(appended);
      } catch (GeneralSecurityException | SeedValidationException e) {
        logger.error("error importing seed from storage");
        return;
      }
    }

    if (data.getSubscriptions().equals(networkManager.getMessageSender().getSubscriptions())) {
      logger.debug("current state is up to date");
      return;
    }
    logger.debug("restoring connections");
    networkManager.connect(data.getServerAddress(), data.getSubscriptions());
    getApplication()
        .startActivity(
            ConnectingActivity.newIntentForHome(getApplication().getApplicationContext()));
  }

  public void savePersistentData() throws GeneralSecurityException {
    ActivityUtils.activitySavingRoutine(
        networkManager, wallet, getApplication().getApplicationContext());
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
