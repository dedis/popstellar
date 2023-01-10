package com.github.dedis.popstellar.ui.socialmedia;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.fragment.app.*;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.SocialMediaActivityBinding;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.digitalcash.DigitalCashActivity;
import com.github.dedis.popstellar.ui.home.HomeActivity;
import com.github.dedis.popstellar.ui.navigation.MainMenuTab;
import com.github.dedis.popstellar.ui.navigation.NavigationActivity;
import com.github.dedis.popstellar.utility.ActivityUtils;

import java.security.GeneralSecurityException;
import java.util.function.Supplier;

import dagger.hilt.android.AndroidEntryPoint;

/** Activity for the social media */
@AndroidEntryPoint
public class SocialMediaActivity extends NavigationActivity {

  private SocialMediaViewModel viewModel;
  private SocialMediaActivityBinding binding;

  public static final String TAG = SocialMediaActivity.class.getSimpleName();
  public static final String LAO_ID = "LAO_ID";
  public static final String LAO_NAME = "LAO_NAME";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    binding = SocialMediaActivityBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    navigationViewModel = viewModel = obtainViewModel(this);

    // When we launch the social media from a lao, it directly sets its id and name
    if (getIntent().getExtras() != null) {
      String laoId = getIntent().getExtras().getString(LAO_ID);
      String laoName = getIntent().getExtras().getString(LAO_NAME);

      if (laoId != null) {
        viewModel.setLaoId(laoId);
      }
      if (laoName != null) {
        viewModel.setLaoName(laoName);
      }
    }
    navigationViewModel.setCurrentTab(MainMenuTab.SOCIAL_MEDIA);
    setupDrawer(
        binding.socialMediaNavigationDrawer,
        binding.socialMediaAppBar,
        binding.socialMediaDrawerLayout);
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

  public static SocialMediaViewModel obtainViewModel(FragmentActivity activity) {
    return new ViewModelProvider(activity).get(SocialMediaViewModel.class);
  }

  private void setupBottomNavBar() {
    viewModel
        .getBottomNavigationTab()
        .observe(this, tab -> binding.socialMediaNavBar.setSelectedItemId(tab.getMenuId()));

    binding.socialMediaNavBar.setOnItemSelectedListener(
        item -> {
          SocialMediaTab tab = SocialMediaTab.findByMenu(item.getItemId());
          Log.i(TAG, "Opening tab : " + tab.getName());
          openBottomTab(tab);
          return true;
        });

    binding.socialMediaNavBar.setOnItemReselectedListener(item -> {});
    viewModel.setBottomNavigationTab(SocialMediaTab.HOME);
  }

  private void openBottomTab(SocialMediaTab tab) {
    switch (tab) {
      case HOME:
        openHomeTab();
        break;
      case SEARCH:
        openSearchTab();
        break;
      case FOLLOWING:
        openFollowingTab();
        break;
      case PROFILE:
        openProfileTab();
        break;
    }
  }

  @Override
  protected boolean openTab(MainMenuTab tab) {
    switch (tab) {
      case INVITE:
        openInviteTab();
        return false;
      case EVENTS:
        openEventsTab();
        return false;
      case SOCIAL_MEDIA:
        return false;
      case DIGITAL_CASH:
        openDigitalCash();
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
        R.id.fragment_social_media_home,
        SocialMediaHomeFragment::newInstance);
  }

  private void openSearchTab() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_social_media_search,
        SocialMediaSearchFragment::newInstance);
  }

  private void openFollowingTab() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_social_media_following,
        SocialMediaFollowingFragment::newInstance);
  }

  private void openProfileTab() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_social_media_profile,
        SocialMediaProfileFragment::newInstance);
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

  private void openDigitalCash() {
    startActivity(DigitalCashActivity.newIntent(this, viewModel.getLaoId()));
  }

  public static Intent newIntent(Context ctx, String laoId, String laoName) {
    Intent intent = new Intent(ctx, SocialMediaActivity.class);
    intent.putExtra(LAO_ID, laoId);
    intent.putExtra(LAO_NAME, laoName);
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
        manager, R.id.fragment_container_social_media, id, fragmentSupplier);
  }
}
