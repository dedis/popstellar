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
public abstract class NavigationViewModel extends AndroidViewModel {

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

  protected NavigationViewModel(@NonNull Application application) {
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
    Role role = determineRole();
    if (this.role.getValue() != role) {
      this.role.setValue(role);
    }
  }

  public void setLaoId(String laoId) {
    this.laoId = laoId;
  }

  public void setIsOrganizer(boolean isOrganizer) {
    this.isOrganizer = isOrganizer;
  }

  public void setIsWitness(boolean isWitness) {
    if (this.isWitness.getValue() != isWitness) {
      this.isWitness.setValue(isWitness);
    }
  }

  public void setIsAttendee(boolean isAttendee) {
    if (this.isAttendee.getValue() != isAttendee) {
      this.isAttendee.setValue(isAttendee);
    }
  }

  public void setIsTab(boolean isTab) {
    if (this.isTab.getValue() != isTab) {
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
    if (isWitness.getValue()) {
      return Role.WITNESS;
    }
    if (isAttendee.getValue()) {
      return Role.ATTENDEE;
    }
    return Role.MEMBER;
  }
}
