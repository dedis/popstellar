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
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.navigation.NavigationActivity;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.Constants;
import com.github.dedis.popstellar.utility.error.ErrorUtils;

import java.security.GeneralSecurityException;
import java.util.Objects;
import java.util.function.Supplier;

import dagger.hilt.android.AndroidEntryPoint;

/** Activity for the digital cash */
@AndroidEntryPoint
public class DigitalCashActivity extends NavigationActivity<DigitalCashTab> {
  private DigitalCashViewModel viewModel;
  private DigitalCashMainActivityBinding binding;
  public static final String TAG = DigitalCashActivity.class.getSimpleName();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = DigitalCashMainActivityBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    navigationViewModel = viewModel = obtainViewModel(this);
    setupNavigationBar(findViewById(R.id.digital_cash_nav_bar));
    setupTopAppBar();

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
      viewModel.subscribeToLao(id);
      viewModel.setLaoId(id);
      viewModel.setRollCallId(getIntent().getExtras().getString(Constants.ROLL_CALL_ID, ""));
    }
  }

  public void openLao() {
    startActivity(
        LaoDetailActivity.newIntentForLao(
            this, Objects.requireNonNull(viewModel.getCurrentLao().getValue()).getId()));
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
    viewModel.setPageTitle(R.string.digital_cash_home);
  }

  private void openHistoryTab() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_digital_cash_history,
        DigitalCashHistoryFragment::newInstance);
    viewModel.setPageTitle(R.string.digital_cash_history);
  }

  private void openSendTab() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_digital_cash_send,
        DigitalCashSendFragment::newInstance);
    viewModel.setPageTitle(R.string.digital_cash_send);
  }

  private void openReceiveTab() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_digital_cash_receive,
        DigitalCashReceiveFragment::newInstance);
    viewModel.setPageTitle(R.string.digital_cash_receive);
  }

  private boolean openIssueTab() {
    PublicKey organizerKey =
        Objects.requireNonNull(viewModel.getCurrentLao().getValue()).getOrganizer();
    PublicKey myKey = viewModel.getKeyManager().getMainPublicKey();

    if (!myKey.equals(organizerKey)) {
      ErrorUtils.logAndShow(this, TAG, R.string.digital_cash_non_organizer_error_issue);
      return false;
    }

    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_digital_cash_issue,
        DigitalCashIssueFragment::newInstance);
    viewModel.setPageTitle(R.string.digital_cash_issue);

    return true;
  }

  private void setupTopAppBar() {
    viewModel.getPageTitle().observe(this, binding.digitalCashAppBar::setTitle);

    binding.digitalCashAppBar.setNavigationOnClickListener(
        v -> {
          Fragment fragment =
              getSupportFragmentManager().findFragmentById(R.id.fragment_container_digital_cash);
          if (fragment instanceof DigitalCashHomeFragment) {
            openLao();
          } else {
            viewModel.setCurrentTab(DigitalCashTab.HOME);
          }
        });
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
