package com.github.dedis.popstellar.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.fragment.app.*;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.LaoDetailActivityBinding;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.detail.event.eventlist.EventListFragment;
import com.github.dedis.popstellar.ui.detail.token.TokenListFragment;
import com.github.dedis.popstellar.ui.detail.witness.WitnessingFragment;
import com.github.dedis.popstellar.ui.digitalcash.DigitalCashActivity;
import com.github.dedis.popstellar.ui.home.HomeActivity;
import com.github.dedis.popstellar.ui.navigation.LaoActivity;
import com.github.dedis.popstellar.ui.navigation.MainMenuTab;
import com.github.dedis.popstellar.ui.socialmedia.SocialMediaHomeFragment;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.Constants;
import com.google.gson.Gson;

import java.security.GeneralSecurityException;
import java.util.Objects;
import java.util.function.Supplier;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LaoDetailActivity extends LaoActivity {

  private static final String TAG = LaoDetailActivity.class.getSimpleName();

  private LaoDetailViewModel viewModel;

  @Inject Gson gson;
  @Inject GlobalNetworkManager networkManager;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    LaoDetailActivityBinding binding = LaoDetailActivityBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    laoViewModel = viewModel = obtainViewModel(this);

    String laoId =
        Objects.requireNonNull(getIntent().getExtras()).getString(Constants.LAO_ID_EXTRA);
    viewModel.subscribeToLao(laoId);

    initializeLaoActivity(
        laoId,
        binding.laoDetailNavigationDrawer,
        binding.laoTopAppBar,
        binding.laoDetailDrawerLayout);

    MainMenuTab tab = (MainMenuTab) getIntent().getExtras().get(Constants.TAB_EXTRA);
    if (tab == null) {
      tab = MainMenuTab.EVENTS;
    }
    laoViewModel.setCurrentTab(tab);
    openTab(tab);
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

  @Override
  public void onBackPressed() {
    openEventsTab();
  }

  @Override
  protected boolean openTab(MainMenuTab tab) {
    switch (tab) {
      case INVITE:
        openInviteTab();
        return true;
      case EVENTS:
        openEventsTab();
        return true;
      case TOKENS:
        openTokensTab();
        return true;
      case WITNESSING:
        openWitnessTab();
        return true;
      case DIGITAL_CASH:
        openDigitalCashTab();
        return false;
      case SOCIAL_MEDIA:
        openSocialMediaTab();
        return false;
      case DISCONNECT:
        startActivity(HomeActivity.newIntent(this));
        return false;
      default:
        Log.w(TAG, "Unhandled tab type : " + tab);
        return false;
    }
  }

  private void openInviteTab() {
    setCurrentFragment(
        getSupportFragmentManager(), R.id.fragment_invite, InviteFragment::newInstance);
  }

  private void openEventsTab() {
    setCurrentFragment(
        getSupportFragmentManager(), R.id.fragment_event_list, EventListFragment::newInstance);
  }

  private void openTokensTab() {
    setCurrentFragment(
        getSupportFragmentManager(), R.id.fragment_tokens, TokenListFragment::newInstance);
  }

  private void openWitnessTab() {
    setCurrentFragment(
        getSupportFragmentManager(), R.id.fragment_witnessing, WitnessingFragment::newInstance);
  }

  private void openDigitalCashTab() {
    startActivity(DigitalCashActivity.newIntent(this, viewModel.getLaoId()));
  }

  private void openSocialMediaTab() {
    startActivity(SocialMediaHomeFragment.newIntent(this, viewModel.getLaoId()));
  }

  public static LaoDetailViewModel obtainViewModel(FragmentActivity activity) {
    return new ViewModelProvider(activity).get(LaoDetailViewModel.class);
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

  public static Intent newIntentWithTab(Context ctx, String laoId, MainMenuTab tab) {
    Intent intent = LaoDetailActivity.newIntentForLao(ctx, laoId);
    intent.putExtra(Constants.TAB_EXTRA, tab);
    return intent;
  }

  public static Intent newIntentForWallet(Context ctx, String laoId) {
    Intent intent = new Intent(ctx, LaoDetailActivity.class);
    intent.putExtra(Constants.LAO_ID_EXTRA, laoId);
    intent.putExtra(Constants.FRAGMENT_TO_OPEN_EXTRA, Constants.CONTENT_WALLET_EXTRA);
    return intent;
  }
}
