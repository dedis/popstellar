package com.github.dedis.popstellar.ui.navigation;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.*;

import com.github.dedis.popstellar.model.Role;

/**
 * Abstract view model that provides the components of the navigation bar
 *
 * <p>Any activity that uses a navigation bar should have its view model extends from the this *
 * class
 */
public abstract class NavigationViewModel extends AndroidViewModel {

  private final MutableLiveData<MainMenuTab> currentTab = new MutableLiveData<>();

  private final MutableLiveData<String> laoName = new MutableLiveData<>();

  private final MutableLiveData<Boolean> isOrganizer = new MutableLiveData<>(Boolean.FALSE);

  private final MutableLiveData<Boolean> isWitness = new MutableLiveData<>(Boolean.FALSE);

  private final MutableLiveData<Boolean> isAttendee = new MutableLiveData<>(Boolean.FALSE);

  private final MutableLiveData<Role> role = new MutableLiveData<>(Role.MEMBER);

  private final MutableLiveData<Boolean> isTab = new MutableLiveData<>(Boolean.TRUE);

  private final MutableLiveData<Integer> pageTitle = new MutableLiveData<>(0);

  protected NavigationViewModel(@NonNull Application application) {
    super(application);
  }

  public LiveData<MainMenuTab> getCurrentTab() {
    return currentTab;
  }

  public void setCurrentTab(MainMenuTab tab) {
    currentTab.setValue(tab);
  }

  public MutableLiveData<String> getLaoName() {
    return laoName;
  }

  public void setLaoName(String laoName) {
    this.laoName.setValue(laoName);
  }

  public MutableLiveData<Boolean> isOrganizer() {
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

  public void updateRole() {
    Role role = determineRole();
    if (this.role.getValue() != role) {
      this.role.setValue(role);
    }
  }

  public void setIsOrganizer(boolean isOrganizer) {
    if (this.isOrganizer.getValue() != isOrganizer) {
      this.isOrganizer.setValue(isOrganizer);
    }
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

  private Role determineRole() {
    if (isOrganizer.getValue()) {
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
