package com.github.dedis.popstellar.ui.lao;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.*;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.Role;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.Wallet;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.RollCallRepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.PopViewModel;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.error.keys.*;
import com.github.dedis.popstellar.utility.security.KeyManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.GeneralSecurityException;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

@HiltViewModel
public class LaoViewModel extends AndroidViewModel implements PopViewModel {
  private static final Logger logger = LogManager.getLogger(LaoViewModel.class);

  private final MutableLiveData<MainMenuTab> currentTab = new MutableLiveData<>();

  private boolean isOrganizer = false;
  private String laoId;

  private final MutableLiveData<Boolean> isWitness = new MutableLiveData<>(Boolean.FALSE);
  private final MutableLiveData<Boolean> isAttendee = new MutableLiveData<>(Boolean.FALSE);
  private final MutableLiveData<Role> role = new MutableLiveData<>(Role.MEMBER);
  private final MutableLiveData<Boolean> isTab = new MutableLiveData<>(Boolean.TRUE);
  private final MutableLiveData<Integer> pageTitle = new MutableLiveData<>(0);

  private final CompositeDisposable disposables = new CompositeDisposable();

  /*
   * Dependencies for this class
   */
  private final LAORepository laoRepo;
  private final RollCallRepository rollCallRepo;
  private final GlobalNetworkManager networkManager;
  private final KeyManager keyManager;
  private final Wallet wallet;

  @Inject
  public LaoViewModel(
      @NonNull Application application,
      LAORepository laoRepository,
      RollCallRepository rollCallRepo,
      GlobalNetworkManager networkManager,
      KeyManager keyManager,
      Wallet wallet) {
    super(application);
    this.laoRepo = laoRepository;
    this.rollCallRepo = rollCallRepo;
    this.networkManager = networkManager;
    this.keyManager = keyManager;
    this.wallet = wallet;
  }

  @Override
  public String getLaoId() {
    return laoId;
  }

  public LaoView getLao() throws UnknownLaoException {
    return laoRepo.getLaoView(laoId);
  }

  public LiveData<MainMenuTab> getCurrentTab() {
    return currentTab;
  }

  public void setCurrentTab(MainMenuTab tab) {
    currentTab.setValue(tab);
  }

  public boolean isOrganizer() {
    return isOrganizer;
  }

  public MutableLiveData<Boolean> isWitness() {
    return isWitness;
  }

  public MutableLiveData<Boolean> isAttendee() {
    return isAttendee;
  }

  public MutableLiveData<Role> getRole() {
    return role;
  }

  public MutableLiveData<Boolean> isTab() {
    return isTab;
  }

  public MutableLiveData<Integer> getPageTitle() {
    return pageTitle;
  }

  /**
   * Returns the public key or null if an error occurred.
   *
   * @return the public key
   */
  public PublicKey getPublicKey() {
    return keyManager.getMainPublicKey();
  }

  public void setLaoId(String laoId) {
    this.laoId = laoId;
  }

  public void setIsOrganizer(boolean isOrganizer) {
    this.isOrganizer = isOrganizer;
  }

  public void setIsWitness(boolean isWitness) {
    if (!Boolean.valueOf(isWitness).equals(this.isWitness.getValue())) {
      this.isWitness.setValue(isWitness);
    }
  }

  public void setIsAttendee(boolean isAttendee) {
    if (!Boolean.valueOf(isAttendee).equals(this.isAttendee.getValue())) {
      this.isAttendee.setValue(isAttendee);
    }
  }

  public void setIsTab(boolean isTab) {
    if (!Boolean.valueOf(isTab).equals(this.isTab.getValue())) {
      this.isTab.setValue(isTab);
    }
  }

  @Override
  public void setPageTitle(@StringRes int pageTitle) {
    if (!this.pageTitle.getValue().equals(pageTitle)) {
      this.pageTitle.setValue(pageTitle);
    }
  }

  public PoPToken getCurrentPopToken(RollCall rollCall) throws KeyException, UnknownLaoException {
    return keyManager.getPoPToken(getLao(), rollCall);
  }

  protected void updateRole() {
    Role currentRole = determineRole();
    if (role.getValue() != currentRole) {
      role.setValue(currentRole);
    }
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
    disposables.add(disposable);
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    disposables.dispose();
  }

  public void savePersistentData() throws GeneralSecurityException {
    ActivityUtils.activitySavingRoutine(
        networkManager, wallet, getApplication().getApplicationContext());
  }

  protected Role determineRole() {
    if (isOrganizer) {
      return Role.ORGANIZER;
    }
    if (Boolean.TRUE.equals(isWitness.getValue())) {
      return Role.WITNESS;
    }
    if (Boolean.TRUE.equals(isAttendee.getValue())) {
      return Role.ATTENDEE;
    }
    return Role.MEMBER;
  }

  protected void observeLao(String laoId) {
    addDisposable(
        laoRepo
            .getLaoObservable(laoId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                laoView -> {
                  logger.debug("got an update for lao: " + laoView);

                  setIsOrganizer(laoView.getOrganizer().equals(keyManager.getMainPublicKey()));
                  setIsWitness(laoView.getWitnesses().contains(keyManager.getMainPublicKey()));

                  updateRole();
                },
                error -> logger.debug("error updating LAO :" + error)));
  }

  protected void observeRollCalls(String laoId) {
    addDisposable(
        rollCallRepo
            .getRollCallsObservableInLao(laoId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                rollCalls -> {
                  boolean isLastRollCallAttended =
                      rollCalls.stream()
                          .filter(rc -> isRollCallAttended(rc, laoId))
                          .anyMatch(
                              rc -> {
                                try {
                                  return rc.equals(rollCallRepo.getLastClosedRollCall(laoId));
                                } catch (NoRollCallException e) {
                                  return false;
                                }
                              });
                  setIsAttendee(isLastRollCallAttended);
                },
                error ->
                    ErrorUtils.logAndShow(
                        getApplication(), logger, error, R.string.unknown_roll_call_exception)));
  }

  private boolean isRollCallAttended(RollCall rollcall, String laoId) {
    try {
      PublicKey pk = wallet.generatePoPToken(laoId, rollcall.getPersistentId()).getPublicKey();
      return rollcall.isClosed() && rollcall.getAttendees().contains(pk);
    } catch (KeyGenerationException | UninitializedWalletException e) {
      logger.error("failed to retrieve public key from wallet", e);
      return false;
    }
  }
}
