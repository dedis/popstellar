package com.github.dedis.popstellar.ui.digitalcash;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.*;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.navigation.NavigationActivity;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.Constants;
import com.github.dedis.popstellar.utility.error.ErrorUtils;

import java.util.function.Supplier;

import dagger.hilt.android.AndroidEntryPoint;

/** Activity for the digital cash */
@AndroidEntryPoint
public class DigitalCashActivity extends NavigationActivity<DigitalCashTab> {
  private DigitalCashViewModel viewModel;
  public static final String TAG = DigitalCashActivity.class.getSimpleName();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.digital_cash_main_activity);
    navigationViewModel = viewModel = obtainViewModel(this);
    setupNavigationBar(findViewById(R.id.digital_cash_nav_bar));
    setupBackButton();

    loadIntentData();
  }

  @Override
  protected void onPause() {
    // Done in onPause because it is the only lifecycle "end" method guaranteed to be called in all
    // circumstances
    super.onPause();
    viewModel.savePersistentData();
  }

  public void loadIntentData() {
    if (getIntent().getExtras() != null) {
      String id = getIntent().getExtras().getString(Constants.LAO_ID_EXTRA, "");
      viewModel.subscribeToLao(id);
      viewModel.setLaoId(id);
      viewModel.setLaoName(getIntent().getExtras().getString(Constants.LAO_NAME, ""));
      viewModel.setRollCallId(getIntent().getExtras().getString(Constants.ROLL_CALL_ID, ""));
    }
  }

  public void openLao() {
    startActivity(
        LaoDetailActivity.newIntentForLao(this, viewModel.getCurrentLao().getValue().getId()));
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
    if (menuItem.getItemId() == android.R.id.home) {
      Fragment fragment =
          getSupportFragmentManager().findFragmentById(R.id.fragment_container_digital_cash);
      if (fragment instanceof DigitalCashHomeFragment) {
        openLao();
      } else {
        viewModel.setCurrentTab(DigitalCashTab.HOME);
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

  @Override
  protected DigitalCashTab findTabByMenu(int menuId) {
    return DigitalCashTab.findByMenu(menuId);
  }

  @Override
  protected boolean openTab(DigitalCashTab tab) {
    switch (tab) {
      case HOME:
        openHomeTab();
        break;
      case HISTORY:
        openHistoryTab();
        break;
      case SEND:
        openSendTab();
        break;
      case RECEIVE:
        openReceiveTab();
        break;
      case ISSUE:
        return openIssueTab();
      default:
        Log.w(TAG, "Unhandled tab type : " + tab);
    }
    return true;
  }

  @Override
  protected DigitalCashTab getDefaultTab() {
    return DigitalCashTab.HOME;
  }

  private void openHomeTab() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_digital_cash_home,
        DigitalCashHomeFragment::newInstance);
  }

  private void openHistoryTab() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_digital_cash_history,
        DigitalCashHistoryFragment::newInstance);
  }

  private void openSendTab() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_digital_cash_send,
        DigitalCashSendFragment::newInstance);
  }

  private void openReceiveTab() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_digital_cash_receive,
        DigitalCashReceiveFragment::newInstance);
  }

  private boolean openIssueTab() {
    PublicKey organizerKey = viewModel.getCurrentLao().getValue().getOrganizer();
    PublicKey myKey = viewModel.getKeyManager().getMainPublicKey();

    if (!myKey.equals(organizerKey)) {
      ErrorUtils.logAndShow(this, TAG, R.string.digital_cash_non_organizer_error_issue);
      return false;
    }

    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_digital_cash_issue,
        DigitalCashIssueFragment::newInstance);
    return true;
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
