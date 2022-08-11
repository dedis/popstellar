package com.github.dedis.popstellar.ui.navigation;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.*;

/**
 * Abstract view model that provides the components of the navigation bar
 *
 * <p>Any activity that uses a navigation bar should have its view model extends from the this *
 * class
 *
 * @param <T> the type fo the Tab used
 */
public abstract class NavigationViewModel<T extends Tab> extends AndroidViewModel {

  private final MutableLiveData<T> currentTab = new MutableLiveData<>();

  protected NavigationViewModel(@NonNull Application application) {
    super(application);
  }

  public LiveData<T> getCurrentTab() {
    return currentTab;
  }

  public void setCurrentTab(T tab) {
    currentTab.postValue(tab);
  }
}
