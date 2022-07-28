package com.github.dedis.popstellar.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.*;
import androidx.appcompat.app.*;
import androidx.fragment.app.*;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.ui.detail.event.consensus.ElectionStartFragment;
import com.github.dedis.popstellar.ui.detail.event.rollcall.*;
import com.github.dedis.popstellar.ui.detail.witness.WitnessingFragment;
import com.github.dedis.popstellar.ui.wallet.LaoWalletFragment;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.Constants;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Objects;
import java.util.function.Supplier;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LaoDetailActivity extends AppCompatActivity {

  private LaoDetailViewModel mViewModel;
  private BottomNavigationView navbar;

  public static LaoDetailViewModel obtainViewModel(FragmentActivity activity) {
    return new ViewModelProvider(activity).get(LaoDetailViewModel.class);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.lao_detail_activity);
    mViewModel = obtainViewModel(this);

    navbar = findViewById(R.id.lao_detail_nav_bar);
    setupNavigationBar();

    setupBackButton();

    mViewModel.subscribeToLao(
        (String) Objects.requireNonNull(getIntent().getExtras()).get(Constants.LAO_ID_EXTRA));
    if (getIntent()
        .getExtras()
        .get(Constants.FRAGMENT_TO_OPEN_EXTRA)
        .equals(Constants.LAO_DETAIL_EXTRA)) {
      mViewModel.openLaoDetail(getSupportFragmentManager());
    } else {
      setupLaoWalletFragment();
    }

    // Subscribe to "enter roll call" event
    mViewModel
        .getPkRollCallEvent()
        .observe(
            this,
            publicKeySingleEvent -> {
              PublicKey pk = publicKeySingleEvent.getContentIfNotHandled();
              if (pk != null) {
                enterRollCall(pk);
              }
            });

    mViewModel
        .getCloseRollCallEvent()
        .observe(
            this,
            booleanSingleEvent -> {
              Boolean event = booleanSingleEvent.getContentIfNotHandled();
              if (event != null) {
                mViewModel.openLaoDetail(getSupportFragmentManager());
              }
            });

    subscribeWalletEvents();

    // Subscribe to "open start election" event
    setupElectionStartFragment();
  }

  private void subscribeWalletEvents() {
    mViewModel
        .getWalletMessageEvent()
        .observe(
            this,
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setUpWalletMessage();
              }
            });
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
    if (menuItem.getItemId() == android.R.id.home) {
      Fragment fragment =
          getSupportFragmentManager().findFragmentById(R.id.fragment_container_lao_detail);
      if (fragment instanceof LaoDetailFragment) {
        mViewModel.openHome(this);
      } else {
        navbar.setSelectedItemId(R.id.lao_detail_event_list_menu);
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

  private void setupLaoWalletFragment() {
    setCurrentFragment(
        getSupportFragmentManager(), R.id.fragment_lao_wallet, LaoWalletFragment::newInstance);
  }

  private static void setupWitnessingFragment(FragmentManager manager) {
    setCurrentFragment(manager, R.id.fragment_witnessing, WitnessingFragment::newInstance);
  }

  private void setupRollCallTokenFragment(String id) {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_rollcall_token,
        () -> RollCallTokenFragment.newInstance(id));
  }

  private void setupAttendeesListFragment(String id) {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_attendees_list,
        () -> AttendeesListFragment.newInstance(id));
  }

  private void enterRollCall(PublicKey pk) {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_roll_call,
        () -> RollCallFragment.newInstance(pk));
  }

  public void setUpWalletMessage() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("You have to setup up your wallet before connecting.");
    builder.setPositiveButton("Ok", (dialog, which) -> dialog.dismiss());
    builder.show();
  }

  private void setupElectionStartFragment() {
    mViewModel
        .getOpenStartElectionEvent()
        .observe(
            this,
            booleanSingleEvent -> {
              Boolean event = booleanSingleEvent.getContentIfNotHandled();
              if (event != null) {
                setCurrentFragment(
                    getSupportFragmentManager(),
                    R.id.fragment_election_start,
                    ElectionStartFragment::newInstance);
              }
            });
  }

  public void setupNavigationBar() {
    navbar.setOnItemSelectedListener(
        item -> {
          int id = item.getItemId();
          if (id == R.id.lao_detail_event_list_menu) {
            mViewModel.openLaoDetail(getSupportFragmentManager());
          } else if (id == R.id.lao_detail_identity_menu) {
            mViewModel.openIdentity(getSupportFragmentManager());
          } else if (id == R.id.lao_detail_witnessing_menu) {
            mViewModel.openWitnessing(getSupportFragmentManager());
          } else if (id == R.id.lao_detail_digital_cash_menu) {
            mViewModel.openDigitalCash(this);
          } else if (id == R.id.lao_detail_social_media_menu) {
            mViewModel.openSocialMedia(this);
          }
          return true;
        });
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
