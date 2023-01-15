package com.github.dedis.popstellar.ui.navigation;

import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.Role;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.Wallet;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.RollCallRepository;
import com.github.dedis.popstellar.ui.home.HomeActivity;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.error.keys.*;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * This abstract activity encapsulate the redundant behavior of an activity with a navigation bar
 *
 * <p>An activity extending this must instantiate the navigationViewModel in its onCreate and it
 * should call initializeLaoActivity with the navigationView as parameter.
 */
@AndroidEntryPoint
public abstract class LaoActivity extends AppCompatActivity {

  private static final String TAG = LaoActivity.class.getSimpleName();

  protected LaoViewModel laoViewModel;

  @Inject RollCallRepository rollCallRepo;
  @Inject LAORepository laoRepo;
  @Inject KeyManager keyManager;
  @Inject Wallet wallet;

  /**
   * Setup the navigation bar listeners given the navigation bar view
   *
   * <p>This function should be called in the activity's onCreate after the navigation view model
   * has been set
   *
   * @param navigationView the view
   */
  protected void initializeLaoActivity(
      String laoId,
      NavigationView navigationView,
      MaterialToolbar toolbar,
      DrawerLayout drawerLayout) {

    laoViewModel.setLaoId(laoId);
    observeRoles();

    // Listen to click on left icon of toolbar
    toolbar.setNavigationOnClickListener(
        view -> {
          if (Boolean.TRUE.equals(laoViewModel.isTab().getValue())) {
            // If it is a tab open menu
            drawerLayout.openDrawer(GravityCompat.START);
          } else {
            // Press back arrow
            onBackPressed();
          }
        });

    // Observe whether the menu icon or back arrow should be displayed
    laoViewModel
        .isTab()
        .observe(
            this,
            isTab ->
                toolbar.setNavigationIcon(
                    Boolean.TRUE.equals(isTab)
                        ? R.drawable.menu_icon
                        : R.drawable.back_arrow_icon));

    // Observe changes to the tab selected
    laoViewModel
        .getCurrentTab()
        .observe(
            this,
            tab -> {
              laoViewModel.setIsTab(true);
              navigationView.setCheckedItem(tab.getMenuId());
            });

    navigationView.setNavigationItemSelectedListener(
        item -> {
          MainMenuTab tab = MainMenuTab.findByMenu(item.getItemId());
          Log.i(TAG, "Opening tab : " + tab.getName());
          boolean selected = openTab(tab);
          if (selected) {
            Log.d(TAG, "The tab was successfully opened");
            laoViewModel.setCurrentTab(tab);
          } else {
            Log.d(TAG, "The tab wasn't opened");
          }
          drawerLayout.close();
          return selected;
        });

    observeLao(laoId);
    try {
      setupHeaderLaoName(navigationView, laoViewModel.getLao().getName());
    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(this, TAG, e, R.string.unknown_lao_exception);
      startActivity(HomeActivity.newIntent(this));
    }

    // Update the user's role in the drawer header when it changes
    laoViewModel.getRole().observe(this, role -> setupHeaderRole(navigationView, role));

    // Observe the toolbar title to display
    laoViewModel
        .getPageTitle()
        .observe(
            this,
            resId -> {
              if (resId != 0) {
                toolbar.setTitle(resId);
              }
            });

    observeRollCalls(laoId);
  }

  private void observeRoles() {
    // Observe any change in the following variable to update the role
    laoViewModel.isWitness().observe(this, any -> laoViewModel.updateRole());
    laoViewModel.isAttendee().observe(this, any -> laoViewModel.updateRole());
  }

  private void observeRollCalls(String laoId) {
    laoViewModel.addDisposable(
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
                  laoViewModel.setIsAttendee(isLastRollCallAttended);
                },
                error ->
                    ErrorUtils.logAndShow(this, TAG, error, R.string.unknown_roll_call_exception)));
  }

  private void observeLao(String laoId) {
    laoViewModel.addDisposable(
        laoRepo
            .getLaoObservable(laoId)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                laoView -> {
                  Log.d(TAG, "got an update for lao: " + laoView);

                  laoViewModel.setWitnessMessages(
                      new ArrayList<>(laoView.getWitnessMessages().values()));
                  laoViewModel.setWitnesses(new ArrayList<>(laoView.getWitnesses()));

                  boolean isOrganizer =
                      laoView.getOrganizer().equals(keyManager.getMainPublicKey());
                  laoViewModel.setIsOrganizer(isOrganizer);
                  laoViewModel.setIsWitness(
                      laoView.getWitnesses().contains(keyManager.getMainPublicKey()));

                  laoViewModel.updateRole();
                },
                error -> Log.d(TAG, "error updating LAO :" + error)));
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

  private boolean isRollCallAttended(RollCall rollcall, String laoId) {
    try {
      PublicKey pk = wallet.generatePoPToken(laoId, rollcall.getPersistentId()).getPublicKey();
      return rollcall.isClosed() && rollcall.getAttendees().contains(pk);
    } catch (KeyGenerationException | UninitializedWalletException e) {
      Log.e(TAG, "failed to retrieve public key from wallet", e);
      return false;
    }
  }

  /**
   * Open the provided tab
   *
   * @param tab to pen
   * @return true if the tab was actually opened and the menu should be selected
   */
  protected abstract boolean openTab(MainMenuTab tab);
}
