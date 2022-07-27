package com.github.dedis.popstellar.ui.digitalcash;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.*;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.Constants;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.function.Supplier;

import dagger.hilt.android.AndroidEntryPoint;

/** Activity for the digital cash */
@AndroidEntryPoint
public class DigitalCashActivity extends AppCompatActivity {
  private DigitalCashViewModel mViewModel;
  public static final String TAG = DigitalCashActivity.class.getSimpleName();
  private BottomNavigationView navbar;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.digital_cash_main_activity);
    mViewModel = obtainViewModel(this);
    setupNavigationBar();
    setupBackButton();

    subscribeToSelectedItemEvents();
    setTheIntent();

    mViewModel.openHome();
  }

  private void subscribeToSelectedItemEvents() {
    mViewModel
        .getCurrentSelectedItem()
        .observe(
            this,
            item -> {
              if (item != null) {
                navbar.setSelectedItemId(item);
              }
            });
  }

  public void setTheIntent() {
    if (getIntent().getExtras() != null) {
      String id = getIntent().getExtras().getString(Constants.LAO_ID_EXTRA, "");
      mViewModel.subscribeToLao(id);
      mViewModel.setLaoId(id);
      mViewModel.setLaoName(getIntent().getExtras().getString(Constants.LAO_NAME, ""));
      mViewModel.setRollCallId(getIntent().getExtras().getString(Constants.ROLL_CALL_ID, ""));
    }
  }

  public void openLao() {
    startActivity(
        LaoDetailActivity.newIntentForLao(this, mViewModel.getCurrentLao().getValue().getId()));
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
    if (menuItem.getItemId() == android.R.id.home) {
      Fragment fragment =
          getSupportFragmentManager().findFragmentById(R.id.fragment_container_digital_cash);
      if (fragment instanceof DigitalCashHomeFragment) {
        openLao();
      } else {
        mViewModel.openHome();
      }
      return true;
    }
    return super.onOptionsItemSelected(menuItem);
  }

  private void setupBackButton() {
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setHomeAsUpIndicator(R.drawable.ic_back_arrow);
      actionBar.setDisplayHomeAsUpEnabled(true);
    }
  }

  public static DigitalCashViewModel obtainViewModel(FragmentActivity activity) {
    return new ViewModelProvider(activity).get(DigitalCashViewModel.class);
  }

  public void setupNavigationBar() {
    navbar = findViewById(R.id.digital_cash_nav_bar);
    navbar.setOnItemSelectedListener(
        item -> {
          int id = item.getItemId();
          if (id != mViewModel.getCurrentSelectedItem().getValue()) {
            // This prevents the update to be done multiple times. It is done here rather than
            // in viewModel because otherwise this would be executed twice
            mViewModel.setCurrentSelectedItem(id);
          } else {
            if (id == R.id.digital_cash_home_menu) {
              Log.d(TAG, "Opening home fragment");
              openHomeFragment();
            } else if (id == R.id.digital_cash_history_menu) {
              Log.d(TAG, "Opening history fragment");
              openHistoryFragment();
            } else if (id == R.id.digital_cash_send_menu) {
              Log.d(TAG, "Opening send fragment");
              openSendFragment();
            } else if (id == R.id.digital_cash_receive_menu) {
              Log.d(TAG, "Opening receive fragment");
              openReceiveFragment();
            } else if (id == R.id.digital_cash_issue_menu) {
              Log.d(TAG, "Trying to open issue fragment");
              handleOpenIssue();
            }
          }
          return true;
        });
  }

  private void openHomeFragment() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_digital_cash_home,
        DigitalCashHomeFragment::newInstance);
  }

  private void openHistoryFragment() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_digital_cash_history,
        DigitalCashHistoryFragment::newInstance);
  }

  private void openSendFragment() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_digital_cash_send,
        DigitalCashSendFragment::newInstance);
  }

  private void openReceiveFragment() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_digital_cash_receive,
        DigitalCashReceiveFragment::newInstance);
  }

  private void handleOpenIssue() {
    PublicKey organizerKey = mViewModel.getCurrentLao().getValue().getOrganizer();
    PublicKey myKey = mViewModel.getKeyManager().getMainPublicKey();
    if (!myKey.equals(organizerKey)) {
      ErrorUtils.logAndShow(this, TAG, R.string.digital_cash_non_organizer_error_issue);
    } else {
      openIssueFragment();
    }
  }

  private void openIssueFragment() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_digital_cash_issue,
        DigitalCashIssueFragment::newInstance);
  }

  public static Intent newIntent(Context ctx, String laoId, String laoName) {
    Intent intent = new Intent(ctx, DigitalCashActivity.class);
    intent.putExtra(Constants.LAO_ID_EXTRA, laoId);
    intent.putExtra(Constants.LAO_NAME, laoName);
    return intent;
  }

  /**
   * Set the current fragment in the container of the activity
   *
   * @param id of the fragment
   * @param fragmentSupplier provides the fragment if it is missing
   */
  public static void setCurrentFragment(
      FragmentManager manager, @IdRes int id, Supplier<Fragment> fragmentSupplier) {
    ActivityUtils.setFragmentInContainer(
        manager, R.id.fragment_container_digital_cash, id, fragmentSupplier);
  }
}
