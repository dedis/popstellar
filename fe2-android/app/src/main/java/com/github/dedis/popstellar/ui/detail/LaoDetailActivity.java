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
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.detail.witness.WitnessingFragment;
import com.github.dedis.popstellar.ui.digitalcash.DigitalCashActivity;
import com.github.dedis.popstellar.ui.home.HomeActivity;
import com.github.dedis.popstellar.ui.navigation.MainMenuTab;
import com.github.dedis.popstellar.ui.navigation.NavigationActivity;
import com.github.dedis.popstellar.ui.socialmedia.SocialMediaActivity;
import com.github.dedis.popstellar.ui.wallet.LaoWalletFragment;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.Constants;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.google.gson.Gson;

import java.security.GeneralSecurityException;
import java.util.Objects;
import java.util.function.Supplier;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LaoDetailActivity extends NavigationActivity {

  private static final String TAG = LaoDetailActivity.class.getSimpleName();

  private LaoDetailViewModel viewModel;
  private LaoDetailActivityBinding binding;

  @Inject Gson gson;
  @Inject GlobalNetworkManager networkManager;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = LaoDetailActivityBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    navigationViewModel = viewModel = obtainViewModel(this);
    navigationViewModel.setCurrentTab(MainMenuTab.EVENTS);
    setupDrawer(
        binding.laoDetailNavigationDrawer, binding.laoTopAppBar, binding.laoDetailDrawerLayout);
    setupTopAppBar();

    String laoId =
        Objects.requireNonNull(getIntent().getExtras()).getString(Constants.LAO_ID_EXTRA);
    viewModel.subscribeToLao(laoId);
    viewModel.subscribeToRollCalls(laoId);

    if (!getIntent()
        .getExtras()
        .get(Constants.FRAGMENT_TO_OPEN_EXTRA)
        .equals(Constants.LAO_DETAIL_EXTRA)) {
      setupLaoWalletFragment();
    }

    openEventsTab();
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

  private void setupTopAppBar() {
    viewModel.getPageTitle().observe(this, binding.laoTopAppBar::setTitle);

    //    binding.laoTopAppBar.setOnMenuItemClickListener(
    //        item -> {
    //          if (item.getItemId() == R.id.lao_toolbar_qr_code) {
    //            binding.laoDetailQrLayout.setVisibility(View.VISIBLE);
    //            binding.laoDetailQrLayout.setAlpha(Constants.ENABLED_ALPHA);
    //            binding.fragmentContainerLaoDetail.setAlpha(Constants.DISABLED_ALPHA);
    //            binding.fragmentContainerLaoDetail.setEnabled(false);
    //            return true;
    //          }
    //          return false;
    //        });
  }

  //  private void setupProperties() {

  @Override
  protected boolean openTab(MainMenuTab tab) {
    switch (tab) {
      case INVITE:
        openInviteTab();
        return true;
      case EVENTS:
        openEventsTab();
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
      case TOKENS:
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
        getSupportFragmentManager(), R.id.fragment_lao_detail, LaoDetailFragment::newInstance);
  }

  private void openWitnessTab() {
    setCurrentFragment(
        getSupportFragmentManager(), R.id.fragment_witnessing, WitnessingFragment::newInstance);
  }

  private void openDigitalCashTab() {
    startActivity(DigitalCashActivity.newIntent(this, viewModel.getLaoId()));
  }

  private void openSocialMediaTab() {
    try {
      LaoView laoView = viewModel.getLaoView();
      startActivity(SocialMediaActivity.newIntent(this, viewModel.getLaoId(), laoView.getName()));
    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(this, TAG, R.string.error_no_lao);
    }
  }

  private void setupLaoWalletFragment() {
    setCurrentFragment(
        getSupportFragmentManager(), R.id.fragment_lao_wallet, LaoWalletFragment::newInstance);
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

  public static Intent newIntentForWallet(Context ctx, String laoId) {
    Intent intent = new Intent(ctx, LaoDetailActivity.class);
    intent.putExtra(Constants.LAO_ID_EXTRA, laoId);
    intent.putExtra(Constants.FRAGMENT_TO_OPEN_EXTRA, Constants.CONTENT_WALLET_EXTRA);
    return intent;
  }
}
