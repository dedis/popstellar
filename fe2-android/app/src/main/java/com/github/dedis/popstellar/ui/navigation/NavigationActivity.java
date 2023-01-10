package com.github.dedis.popstellar.ui.navigation;

import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.Role;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

/**
 * This abstract activity encapsulate the redundant behavior of an activity with a navigation bar
 *
 * <p>An activity extending this must instantiate the navigationViewModel in its onCreate and it
 * should call setupDrawer with the navigationView as parameter.
 */
public abstract class NavigationActivity extends AppCompatActivity {

  private static final String TAG = NavigationActivity.class.getSimpleName();

  protected NavigationViewModel navigationViewModel;

  /**
   * Setup the navigation bar listeners given the navigation bar view
   *
   * <p>This function should be called in the activity's onCreate after the navigation view model
   * has been set
   *
   * @param navigationView the view
   */
  protected void setupDrawer(
      NavigationView navigationView, MaterialToolbar toolbar, DrawerLayout drawerLayout) {

    observeRoles();

    // Listen to click on left icon of toolbar
    toolbar.setNavigationOnClickListener(
        view -> {
          if (navigationViewModel.isTab().getValue()) {
            // If it is a tab open menu
            drawerLayout.openDrawer(GravityCompat.START);
          } else {
            // Press back arrow
            onBackPressed();
          }
        });

    // Observe whether the menu icon or back arrow should be displayed
    navigationViewModel
        .isTab()
        .observe(
            this,
            isTab ->
                toolbar.setNavigationIcon(
                    isTab ? R.drawable.menu_drawer_icon : R.drawable.ic_back_arrow));

    // Observe changes to the tab selected
    navigationViewModel
        .getCurrentTab()
        .observe(
            this,
            tab -> {
              navigationViewModel.setIsTab(true);
              navigationView.setCheckedItem(tab.getMenuId());
            });

    navigationView.setNavigationItemSelectedListener(
        item -> {
          MainMenuTab tab = MainMenuTab.findByMenu(item.getItemId());
          Log.i(TAG, "Opening tab : " + tab.getName());
          boolean selected = openTab(tab);
          if (selected) {
            Log.d(TAG, "The tab was successfully opened");
            navigationViewModel.setCurrentTab(tab);
          } else {
            Log.d(TAG, "The tab wasn't opened");
          }
          drawerLayout.close();
          return selected;
        });

    // Update the name of the Lao in the drawer header when it changes
    navigationViewModel
        .getLaoName()
        .observe(this, laoName -> setupHeaderLaoName(navigationView, laoName));

    // Update the user's role in the drawer header when it changes
    navigationViewModel.getRole().observe(this, role -> setupHeaderRole(navigationView, role));

    // Observe the toolbar title to display
    navigationViewModel
        .getPageTitle()
        .observe(
            this,
            resId -> {
              if (resId != 0) {
                toolbar.setTitle(resId);
              }
            });
  }

  private void observeRoles() {
    // Observe any change in the following variable to update the role
    navigationViewModel.isOrganizer().observe(this, any -> navigationViewModel.updateRole());
    navigationViewModel.isWitness().observe(this, any -> navigationViewModel.updateRole());
    navigationViewModel.isAttendee().observe(this, any -> navigationViewModel.updateRole());
  }

  private void setupHeaderLaoName(NavigationView navigationView, String laoName) {
    TextView laoNameView =
        navigationView
            .getHeaderView(0) // We have only one header
            .findViewById(R.id.drawer_header_lao_title);
    laoNameView.setText(laoName);
  }

  private void setupHeaderRole(NavigationView navigationView, Role role) {
    TextView roleView =
        navigationView
            .getHeaderView(0) // We have only one header
            .findViewById(R.id.drawer_header_role);
    roleView.setText(role.getStringId());
  }

  /**
   * Open the provided tab
   *
   * @param tab to pen
   * @return true if the tab was actually opened and the menu should be selected
   */
  protected abstract boolean openTab(MainMenuTab tab);
}
