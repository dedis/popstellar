package com.github.dedis.popstellar.ui.navigation;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.navigation.NavigationBarView;

/**
 * This abstract activity encapsulate the redundant behavior of an activity with a navigation bar
 *
 * <p>An activity extending this must instantiate the navigationViewModel in its onCreate and it
 * should call setupNavigationBar with the navigationView as parameter.
 */
public abstract class NavigationActivity<T extends Tab> extends AppCompatActivity {

  protected NavigationViewModel<T> navigationViewModel;

  /**
   * Setup the navigation bar listeners given the navigation bar view
   *
   * <p>This function should be called in the activity's onCreate after the navigation view model
   * has been set
   *
   * @param navbar the view
   */
  protected void setupNavigationBar(NavigationBarView navbar) {
    navigationViewModel
        .getCurrentTab()
        .observe(this, tab -> navbar.setSelectedItemId(tab.getMenuId()));
    navbar.setOnItemSelectedListener(
        item -> {
          T tab = findTabByMenu(item.getItemId());
          boolean selected = openTab(tab);
          if (selected) navigationViewModel.setCurrentTab(tab);
          return selected;
        });
    // Set an empty reselect listener to disable the onSelectListener when pressing multiple times
    navbar.setOnItemReselectedListener(item -> {});

    openDefaultTab(navbar);
  }

  private void openDefaultTab(NavigationBarView navbar) {
    T defaultTab = getDefaultTab();
    navigationViewModel.setCurrentTab(defaultTab);

    // If the tab is already selected, the event will not be dispatched and we need to do it
    // manually
    if (defaultTab.getMenuId() == navbar.getSelectedItemId()) {
      openTab(defaultTab);
    }
  }

  /**
   * Returns a Tab instance given its menu id
   *
   * @param menuId of the tab
   * @return the tab
   */
  protected abstract T findTabByMenu(int menuId);

  /**
   * Open the provided tab
   *
   * @param tab to pen
   * @return true if the tab was actually opened and the menu should be selected
   */
  protected abstract boolean openTab(T tab);

  /**
   * @return the tab to open by default
   */
  protected abstract T getDefaultTab();
}
