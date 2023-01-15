package com.github.dedis.popstellar.ui.navigation;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.*;

import com.github.dedis.popstellar.model.Role;
import com.github.dedis.popstellar.model.objects.WitnessMessage;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

/**
 * Abstract view model that provides the components of the navigation bar
 *
 * <p>Any activity that uses a navigation bar should have its view model extends from the this *
 * class
 */
public abstract class LaoViewModel extends AndroidViewModel {

  private final MutableLiveData<MainMenuTab> currentTab = new MutableLiveData<>();

  private boolean isOrganizer = false;
  private String laoId;

  private final MutableLiveData<Boolean> isWitness = new MutableLiveData<>(Boolean.FALSE);
  private final MutableLiveData<Boolean> isAttendee = new MutableLiveData<>(Boolean.FALSE);
  private final MutableLiveData<Role> role = new MutableLiveData<>(Role.MEMBER);
  private final MutableLiveData<Boolean> isTab = new MutableLiveData<>(Boolean.TRUE);
  private final MutableLiveData<Integer> pageTitle = new MutableLiveData<>(0);
  private final MutableLiveData<List<PublicKey>> witnesses =
      new MutableLiveData<>(new ArrayList<>());
  private final MutableLiveData<List<WitnessMessage>> witnessMessages =
      new MutableLiveData<>(new ArrayList<>());

  private final CompositeDisposable disposables = new CompositeDisposable();

  protected LaoViewModel(@NonNull Application application) {
    super(application);
  }

  public String getLaoId() {
    return laoId;
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

  public abstract LaoView getLao() throws UnknownLaoException;

  public LiveData<List<PublicKey>> getWitnesses() {
    return witnesses;
  }

  public LiveData<List<WitnessMessage>> getWitnessMessages() {
    return witnessMessages;
  }

  public void updateRole() {
    Role currentRole = determineRole();
    if (role.getValue() != currentRole) {
      this.role.setValue(currentRole);
    }
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

  public void setPageTitle(@StringRes Integer pageTitle) {
    if (!this.pageTitle.getValue().equals(pageTitle)) {
      this.pageTitle.setValue(pageTitle);
    }
  }

  public void setWitnesses(List<PublicKey> witnesses) {
    this.witnesses.setValue(witnesses);
  }

  public void setWitnessMessages(List<WitnessMessage> messages) {
    this.witnessMessages.setValue(messages);
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

  @Override
  protected void onCleared() {
    super.onCleared();
    disposables.dispose();
  }

  private Role determineRole() {
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
}
