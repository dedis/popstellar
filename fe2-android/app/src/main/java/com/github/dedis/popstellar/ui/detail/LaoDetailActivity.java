package com.github.dedis.popstellar.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.*;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.*;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.ui.detail.witness.WitnessingFragment;
import com.github.dedis.popstellar.ui.digitalcash.DigitalCashActivity;
import com.github.dedis.popstellar.ui.home.HomeActivity;
import com.github.dedis.popstellar.ui.navigation.NavigationActivity;
import com.github.dedis.popstellar.ui.socialmedia.SocialMediaActivity;
import com.github.dedis.popstellar.ui.wallet.LaoWalletFragment;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.Constants;

import java.util.Objects;
import java.util.function.Supplier;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LaoDetailActivity extends NavigationActivity<LaoTab> {

  private static final String TAG = LaoDetailActivity.class.getSimpleName();

  private LaoDetailViewModel viewModel;

  public static LaoDetailViewModel obtainViewModel(FragmentActivity activity) {
    return new ViewModelProvider(activity).get(LaoDetailViewModel.class);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.lao_detail_activity);
    navigationViewModel = viewModel = obtainViewModel(this);

    setupNavigationBar(findViewById(R.id.lao_detail_nav_bar));
    setupBackButton();

    viewModel.subscribeToLao(
        (String) Objects.requireNonNull(getIntent().getExtras()).get(Constants.LAO_ID_EXTRA));
    if (!getIntent()
        .getExtras()
        .get(Constants.FRAGMENT_TO_OPEN_EXTRA)
        .equals(Constants.LAO_DETAIL_EXTRA)) {
      setupLaoWalletFragment();
    }
  }

  @Override
  protected void onPause() {
    // Done in onPause because it is the only lifecycle "end" method guaranteed to be called in all
    // circumstances
    super.onPause();
    mViewModel.savePersistentData();
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
    if (menuItem.getItemId() == android.R.id.home) {
      Fragment fragment =
          getSupportFragmentManager().findFragmentById(R.id.fragment_container_lao_detail);
      if (fragment instanceof LaoDetailFragment) {
        startActivity(HomeActivity.newIntent(this));
      } else {
        viewModel.setCurrentTab(LaoTab.EVENTS);
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

  @Override
  protected LaoTab findTabByMenu(int menuId) {
    return LaoTab.findByMenu(menuId);
  }

  @Override
  protected boolean openTab(LaoTab tab) {
    switch (tab) {
      case EVENTS:
        openEventsTab();
        break;
      case IDENTITY:
        openIdentityTab();
        break;
      case WITNESSING:
        openWitnessTab();
        break;
      case DIGITAL_CASH:
        openDigitalCashTab();
        break;
      case SOCIAL:
        openSocialMediaTab();
        break;
      default:
        Log.w(TAG, "Unhandled tab type : " + tab);
    }
    return true;
  }

  @Override
  protected LaoTab getDefaultTab() {
    return LaoTab.EVENTS;
  }

  private void openEventsTab() {
    setCurrentFragment(
        getSupportFragmentManager(), R.id.fragment_lao_detail, LaoDetailFragment::newInstance);
  }

  private void openIdentityTab() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_identity,
        () -> IdentityFragment.newInstance(viewModel.getPublicKey()));
  }

  private void openWitnessTab() {
    setCurrentFragment(
        getSupportFragmentManager(), R.id.fragment_witnessing, WitnessingFragment::newInstance);
  }

  private void openDigitalCashTab() {
    startActivity(
        DigitalCashActivity.newIntent(
            this,
            viewModel.getCurrentLaoValue().getId(),
            viewModel.getCurrentLaoValue().getName()));
  }

  private void openSocialMediaTab() {
    startActivity(
        SocialMediaActivity.newIntent(
            this,
            viewModel.getCurrentLaoValue().getId(),
            viewModel.getCurrentLaoValue().getName()));
  }

  private void setupLaoWalletFragment() {
    setCurrentFragment(
        getSupportFragmentManager(), R.id.fragment_lao_wallet, LaoWalletFragment::newInstance);
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
        manager, R.id.fragment_container_lao_detail, id, fragmentSupplier);
  }

  public static Intent newIntentForLao(Context ctx, String laoId) {
    Intent intent = new Intent(ctx, LaoDetailActivity.class);
    intent.putExtra(Constants.LAO_ID_EXTRA, laoId);
    intent.putExtra(Constants.FRAGMENT_TO_OPEN_EXTRA, Constants.LAO_DETAIL_EXTRA);
    return intent;
  }

  public static Intent newIntentForWallet(Context ctx, String laoId) {
    Intent intent = new Intent(ctx, LaoDetailActivity.class);
    intent.putExtra(Constants.LAO_ID_EXTRA, laoId);
    intent.putExtra(Constants.FRAGMENT_TO_OPEN_EXTRA, Constants.CONTENT_WALLET_EXTRA);
    return intent;
  }
}
