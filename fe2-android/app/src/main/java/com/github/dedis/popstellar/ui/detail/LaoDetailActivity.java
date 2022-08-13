package com.github.dedis.popstellar.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.*;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.*;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.ui.detail.witness.WitnessingFragment;
import com.github.dedis.popstellar.ui.digitalcash.DigitalCashActivity;
import com.github.dedis.popstellar.ui.home.HomeActivity;
import com.github.dedis.popstellar.ui.socialmedia.SocialMediaActivity;
import com.github.dedis.popstellar.ui.wallet.LaoWalletFragment;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.Constants;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Objects;
import java.util.function.Supplier;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LaoDetailActivity extends AppCompatActivity {

  private static final String TAG = LaoDetailActivity.class.getSimpleName();

  private LaoDetailViewModel mViewModel;

  public static LaoDetailViewModel obtainViewModel(FragmentActivity activity) {
    return new ViewModelProvider(activity).get(LaoDetailViewModel.class);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.lao_detail_activity);
    mViewModel = obtainViewModel(this);

    setupNavigationBar();
    setupBackButton();

    mViewModel.subscribeToLao(
        (String) Objects.requireNonNull(getIntent().getExtras()).get(Constants.LAO_ID_EXTRA));
    if (getIntent()
        .getExtras()
        .get(Constants.FRAGMENT_TO_OPEN_EXTRA)
        .equals(Constants.LAO_DETAIL_EXTRA)) {
      openEventsMenu();
    } else {
      setupLaoWalletFragment();
    }
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
    if (menuItem.getItemId() == android.R.id.home) {
      Fragment fragment =
          getSupportFragmentManager().findFragmentById(R.id.fragment_container_lao_detail);
      if (fragment instanceof LaoDetailFragment) {
        startActivity(HomeActivity.newIntent(this));
      } else {
        mViewModel.setCurrentTab(LaoTab.EVENTS);
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

  public void setupNavigationBar() {
    BottomNavigationView navbar = findViewById(R.id.lao_detail_nav_bar);
    mViewModel.getCurrentTab().observe(this, tab -> navbar.setSelectedItemId(tab.getMenuId()));
    navbar.setOnItemSelectedListener(
        item -> {
          LaoTab tab = LaoTab.findByMenu(item.getItemId());
          openTab(tab);
          return true;
        });
    // Set an empty reselect listener to disable the onSelectListener when pressing multiple times
    navbar.setOnItemReselectedListener(item -> {});
  }

  private void openTab(LaoTab tab) {
    switch (tab) {
      case EVENTS:
        openEventsMenu();
        break;
      case IDENTITY:
        openIdentityMenu();
        break;
      case WITNESSING:
        openWitnessMenu();
        break;
      case DIGITAL_CASH:
        openDigitalCashMenu();
        break;
      case SOCIAL:
        openSocialMediaMenu();
        break;
      default:
        Log.w(TAG, "Unhandled tab type : " + tab);
    }
  }

  private void openEventsMenu() {
    setCurrentFragment(
        getSupportFragmentManager(), R.id.fragment_lao_detail, LaoDetailFragment::newInstance);
  }

  private void openIdentityMenu() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_identity,
        () -> IdentityFragment.newInstance(mViewModel.getPublicKey()));
  }

  private void openWitnessMenu() {
    setCurrentFragment(
        getSupportFragmentManager(), R.id.fragment_witnessing, WitnessingFragment::newInstance);
  }

  private void openDigitalCashMenu() {
    startActivity(
        DigitalCashActivity.newIntent(
            this,
            mViewModel.getCurrentLaoValue().getId(),
            mViewModel.getCurrentLaoValue().getName()));
  }

  private void openSocialMediaMenu() {
    startActivity(
        SocialMediaActivity.newIntent(
            this,
            mViewModel.getCurrentLaoValue().getId(),
            mViewModel.getCurrentLaoValue().getName()));
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
