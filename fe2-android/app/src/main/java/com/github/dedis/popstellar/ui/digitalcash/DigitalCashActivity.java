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
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.home.HomeActivity;
import com.github.dedis.popstellar.ui.navigation.MainMenuTab;
import com.github.dedis.popstellar.ui.navigation.NavigationActivity;
import com.github.dedis.popstellar.ui.socialmedia.SocialMediaActivity;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.Constants;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;

import java.security.GeneralSecurityException;
import java.util.Objects;
import java.util.function.Supplier;

import dagger.hilt.android.AndroidEntryPoint;

/** Activity for the digital cash */
@AndroidEntryPoint
public class DigitalCashActivity extends NavigationActivity {
  private DigitalCashViewModel viewModel;
  private DigitalCashMainActivityBinding binding;
  public static final String TAG = DigitalCashActivity.class.getSimpleName();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = DigitalCashMainActivityBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    navigationViewModel = viewModel = obtainViewModel(this);

    navigationViewModel.setCurrentTab(MainMenuTab.DIGITAL_CASH);
    setupDrawer(
        binding.digitalCashNavigationDrawer,
        binding.digitalCashAppBar,
        binding.digitalCashDrawerLayout);
    setupBottomNavBar();
    openHomeTab();

    loadIntentData();
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

  public void loadIntentData() {
    if (getIntent().getExtras() != null) {
      String id = getIntent().getExtras().getString(Constants.LAO_ID_EXTRA, "");
      viewModel.setLaoId(id);
      viewModel.setRollCallId(getIntent().getExtras().getString(Constants.ROLL_CALL_ID, ""));
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
        return false;
      case EVENTS:
        openEventsTab();
        return false;
      case SOCIAL_MEDIA:
        openSocialMediaTab();
        return false;
      case DIGITAL_CASH:
        return false;
      case WITNESSING:
        openWitnessingTab();
        return false;
      case TOKENS:
        openTokensTab();
        return false;
      case DISCONNECT:
        startActivity(HomeActivity.newIntent(this));
        return false;
      default:
        Log.w(TAG, "Unhandled tab type : " + tab);
        return false;
    }
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
    try {
      PublicKey organizerKey = viewModel.getCurrentLao().getOrganizer();
      if (!viewModel.getOwnKey().equals(organizerKey)) {
        ErrorUtils.logAndShow(this, TAG, R.string.digital_cash_non_organizer_error_issue);
        return false;
      }
    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(this, TAG, e, R.string.unknown_lao_exception);
      return false;
    }

    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_digital_cash_issue,
        DigitalCashIssueFragment::newInstance);
    viewModel.setPageTitle(R.string.digital_cash_issue);

    return true;
  }

  private void openSocialMediaTab() {
    LaoView laoView = viewModel.getCurrentLaoValue();
    startActivity(SocialMediaActivity.newIntent(this, viewModel.getLaoId(), laoView.getName()));
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
