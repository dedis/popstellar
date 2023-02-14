package com.github.dedis.popstellar.ui.digitalcash;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.fragment.app.*;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.DigitalCashMainActivityBinding;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.home.HomeActivity;
import com.github.dedis.popstellar.ui.navigation.LaoActivity;
import com.github.dedis.popstellar.ui.navigation.MainMenuTab;
import com.github.dedis.popstellar.ui.socialmedia.SocialMediaHomeFragment;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.Constants;
import com.github.dedis.popstellar.utility.error.ErrorUtils;

import java.security.GeneralSecurityException;
import java.util.Objects;
import java.util.function.Supplier;

import dagger.hilt.android.AndroidEntryPoint;

/** Activity for the digital cash */
@AndroidEntryPoint
public class DigitalCashActivity extends LaoActivity {
  private DigitalCashViewModel viewModel;
  private DigitalCashMainActivityBinding binding;
  public static final String TAG = DigitalCashActivity.class.getSimpleName();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = DigitalCashMainActivityBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    laoViewModel = viewModel = obtainViewModel(this);
    String laoId = Objects.requireNonNull(getIntent().getStringExtra(Constants.LAO_ID_EXTRA));
    Log.d(TAG, "Opening digitalCash with id " + laoId);
    initializeLaoActivity(
        laoId,
        binding.digitalCashNavigationDrawer,
        binding.digitalCashAppBar,
        binding.digitalCashDrawerLayout);

    laoViewModel.setCurrentTab(MainMenuTab.DIGITAL_CASH);
    setupBottomNavBar();
    openHomeTab();
  }

  @Override
  public void onStop() {
    super.onStop();

    try {
      viewModel.savePersistentData();
    } catch (GeneralSecurityException e) {
      // We do not display the security error to the user
      Log.d(TAG, "Storage was unsuccessful du to wallet error " + e);
      Toast.makeText(this, R.string.error_storage_wallet, Toast.LENGTH_SHORT).show();
    }
  }

  public static DigitalCashViewModel obtainViewModel(FragmentActivity activity) {
    return new ViewModelProvider(activity).get(DigitalCashViewModel.class);
  }

  private void setupBottomNavBar() {
    viewModel
        .getBottomNavigationTab()
        .observe(this, tab -> binding.digitalCashNavBar.setSelectedItemId(tab.getMenuId()));

    binding.digitalCashNavBar.setOnItemSelectedListener(
        item -> {
          DigitalCashTab tab = DigitalCashTab.findByMenu(item.getItemId());
          Log.i(TAG, "Opening tab : " + tab.getName());
          openBottomTab(tab);
          return true;
        });
    binding.digitalCashNavBar.setOnItemReselectedListener(item -> {});
    viewModel.setBottomNavigationTab(DigitalCashTab.HOME);
  }

  private void openBottomTab(DigitalCashTab tab) {
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
        openIssueTab();
    }
  }

  @Override
  protected boolean openTab(MainMenuTab tab) {
    Log.d(TAG, "opening drawer tab: " + tab);
    switch (tab) {
      case INVITE:
        openInviteTab();
        break;
      case EVENTS:
        openEventsTab();
        break;
      case SOCIAL_MEDIA:
        openSocialMediaTab();
        break;
      case DIGITAL_CASH:
        break;
      case WITNESSING:
        openWitnessingTab();
        break;
      case TOKENS:
        openTokensTab();
        break;
      case DISCONNECT:
        startActivity(HomeActivity.newIntent(this));
        break;
      default:
        Log.w(TAG, "Unhandled tab type : " + tab);
    }
    return false;
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

  private void openIssueTab() {
    if (!viewModel.isOrganizer()) {
      ErrorUtils.logAndShow(this, TAG, R.string.digital_cash_non_organizer_error_issue);
    }
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_digital_cash_issue,
        DigitalCashIssueFragment::newInstance);
    viewModel.setPageTitle(R.string.digital_cash_issue);
  }

  private void openSocialMediaTab() {
    startActivity(SocialMediaHomeFragment.newIntent(this, viewModel.getLaoId()));
  }

  private void openInviteTab() {
    startActivity(
        LaoDetailActivity.newIntentWithTab(this, viewModel.getLaoId(), MainMenuTab.INVITE));
  }

  private void openWitnessingTab() {
    startActivity(
        LaoDetailActivity.newIntentWithTab(this, viewModel.getLaoId(), MainMenuTab.WITNESSING));
  }

  private void openTokensTab() {
    startActivity(
        LaoDetailActivity.newIntentWithTab(this, viewModel.getLaoId(), MainMenuTab.TOKENS));
  }

  private void openEventsTab() {
    startActivity(
        LaoDetailActivity.newIntentWithTab(this, viewModel.getLaoId(), MainMenuTab.EVENTS));
  }

  public static Intent newIntent(Context ctx, String laoId) {
    Intent intent = new Intent(ctx, DigitalCashActivity.class);
    intent.putExtra(Constants.LAO_ID_EXTRA, laoId);
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
