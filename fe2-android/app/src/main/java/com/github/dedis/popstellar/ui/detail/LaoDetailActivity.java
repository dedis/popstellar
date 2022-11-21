package com.github.dedis.popstellar.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.*;
import androidx.fragment.app.*;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.LaoDetailActivityBinding;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.model.qrcode.ConnectToLao;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.ui.detail.event.LaoDetailAnimation;
import com.github.dedis.popstellar.ui.detail.witness.WitnessingFragment;
import com.github.dedis.popstellar.ui.digitalcash.DigitalCashActivity;
import com.github.dedis.popstellar.ui.home.HomeActivity;
import com.github.dedis.popstellar.ui.navigation.NavigationActivity;
import com.github.dedis.popstellar.ui.socialmedia.SocialMediaActivity;
import com.github.dedis.popstellar.ui.wallet.LaoWalletFragment;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.Constants;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.google.gson.Gson;

import net.glxn.qrgen.android.QRCode;

import java.security.GeneralSecurityException;
import java.util.Objects;
import java.util.function.Supplier;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LaoDetailActivity extends NavigationActivity<LaoTab> {

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

    setupNavigationBar(findViewById(R.id.lao_detail_nav_bar));
    setupTopAppBar();
    setupProperties();

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

    binding.laoTopAppBar.setNavigationOnClickListener(
        v -> {
          Fragment fragment =
              getSupportFragmentManager().findFragmentById(R.id.fragment_container_lao_detail);
          if (fragment instanceof LaoDetailFragment) {
            startActivity(HomeActivity.newIntent(this));
          } else {
            if (viewModel.getCurrentTab().getValue() == LaoTab.EVENTS) {
              // On reselection the navigation is supposed to do nothing to prevent loops, so we
              // manually change the fragment
              openEventsTab();
            } else {
              viewModel.setCurrentTab(LaoTab.EVENTS);
            }
          }
        });
    binding.laoTopAppBar.setOnMenuItemClickListener(
        item -> {
          if (item.getItemId() == R.id.lao_toolbar_qr_code) {
            binding.laoDetailQrLayout.setVisibility(View.VISIBLE);
            binding.laoDetailQrLayout.setAlpha(Constants.ENABLED_ALPHA);
            binding.fragmentContainerLaoDetail.setAlpha(Constants.DISABLED_ALPHA);
            binding.fragmentContainerLaoDetail.setEnabled(false);
            return true;
          }
          return false;
        });
  }

  private void setupProperties() {
    binding.laoPropertiesIdentifierText.setText(viewModel.getPublicKey().getEncoded());
    binding.laoPropertiesServerText.setText(networkManager.getCurrentUrl());

    viewModel
        .getCurrentLao()
        .observe(
            this,
            laoView -> {
              // Set the QR code
              ConnectToLao data = new ConnectToLao(networkManager.getCurrentUrl(), laoView.getId());
              Bitmap myBitmap = QRCode.from(gson.toJson(data)).bitmap();
              binding.channelQrCode.setImageBitmap(myBitmap);

              binding.laoPropertiesNameText.setText(laoView.getName());
              binding.laoPropertiesRoleText.setText(getRole());
            });

    binding.qrIconClose.setOnClickListener(
        v -> {
          LaoDetailAnimation.fadeOut(binding.laoDetailQrLayout, 1.0f, 0.0f, 500);
          binding.fragmentContainerLaoDetail.setAlpha(Constants.ENABLED_ALPHA);
          binding.fragmentContainerLaoDetail.setEnabled(true);
        });
  }

  @StringRes
  private int getRole() {
    if (Boolean.TRUE.equals(viewModel.isOrganizer().getValue())) {
      return R.string.organizer;
    } else {
      try {
        boolean isWitness = Boolean.TRUE.equals(viewModel.isWitness().getValue());
        return isWitness ? R.string.witness : R.string.attendee;
      } catch (UnknownLaoException e) {
        return 0;
      }
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
        return true;
      case IDENTITY:
        openIdentityTab();
        return true;
      case WITNESSING:
        openWitnessTab();
        return true;
      case DIGITAL_CASH:
        openDigitalCashTab();
        return false;
      case SOCIAL:
        openSocialMediaTab();
        return false;
      default:
        Log.w(TAG, "Unhandled tab type : " + tab);
        return false;
    }
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
