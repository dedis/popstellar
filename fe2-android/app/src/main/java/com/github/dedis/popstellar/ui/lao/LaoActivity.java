package com.github.dedis.popstellar.ui.lao;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.*;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.LaoActivityBinding;
import com.github.dedis.popstellar.model.Role;
import com.github.dedis.popstellar.ui.detail.InviteFragment;
import com.github.dedis.popstellar.ui.detail.LaoDetailFragment;
import com.github.dedis.popstellar.ui.detail.token.TokenListFragment;
import com.github.dedis.popstellar.ui.detail.witness.WitnessingFragment;
import com.github.dedis.popstellar.ui.digitalcash.DigitalCashActivity;
import com.github.dedis.popstellar.ui.home.HomeActivity;
import com.github.dedis.popstellar.ui.navigation.MainMenuTab;
import com.github.dedis.popstellar.ui.socialmedia.SocialMediaActivity;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.Constants;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;

import java.util.Objects;
import java.util.function.Supplier;

public class LaoActivity extends AppCompatActivity {
  public static final String TAG = LaoActivity.class.getSimpleName();

  LaoViewModel viewModel;
  LaoActivityBinding binding;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = LaoActivityBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    viewModel = obtainViewModel(this);

    String laoId =
        Objects.requireNonNull(getIntent().getExtras()).getString(Constants.LAO_ID_EXTRA);
    viewModel.setLaoId(laoId);

    observeRoles();
    observeToolBar();
    observeDrawer();
    setupDrawerHeader();

    viewModel.observeLao(laoId);
    viewModel.observeRollCalls(laoId);
  }

  private void observeRoles() {
    // Observe any change in the following variable to update the role
    viewModel.isWitness().observe(this, any -> viewModel.updateRole());
    viewModel.isAttendee().observe(this, any -> viewModel.updateRole());
  }

  private void observeToolBar() {
    // Listen to click on left icon of toolbar
    binding.laoAppBar.setNavigationOnClickListener(
        view -> {
          if (Boolean.TRUE.equals(viewModel.isTab().getValue())) {
            // If it is a tab open menu
            binding.laoDrawerLayout.openDrawer(GravityCompat.START);
          } else {
            // Press back arrow
            onBackPressed();
          }
        });

    // Observe whether the menu icon or back arrow should be displayed
    viewModel
        .isTab()
        .observe(
            this,
            isTab ->
                binding.laoAppBar.setNavigationIcon(
                    Boolean.TRUE.equals(isTab)
                        ? R.drawable.menu_icon
                        : R.drawable.back_arrow_icon));

    // Observe the toolbar title to display
    viewModel
        .getPageTitle()
        .observe(
            this,
            resId -> {
              if (resId != 0) {
                binding.laoAppBar.setTitle(resId);
              }
            });
  }

  private void observeDrawer() {
    // Observe changes to the tab selected
    viewModel
        .getCurrentTab()
        .observe(
            this,
            tab -> {
              viewModel.setIsTab(true);
              binding.laoNavigationDrawer.setCheckedItem(tab.getMenuId());
            });

    binding.laoNavigationDrawer.setNavigationItemSelectedListener(
        item -> {
          MainMenuTab tab = MainMenuTab.findByMenu(item.getItemId());
          Log.i(TAG, "Opening tab : " + tab.getName());
          boolean selected = openTab(tab);
          if (selected) {
            Log.d(TAG, "The tab was successfully opened");
            viewModel.setCurrentTab(tab);
          } else {
            Log.d(TAG, "The tab wasn't opened");
          }
          binding.laoDrawerLayout.close();
          return selected;
        });
  }

  private void setupDrawerHeader() {
    try {
      TextView laoNameView =
          binding
              .laoNavigationDrawer
              .getHeaderView(0) // We have only one header
              .findViewById(R.id.drawer_header_lao_title);
      laoNameView.setText(viewModel.getLao().getName());
    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(this, TAG, e, R.string.unknown_lao_exception);
      startActivity(HomeActivity.newIntent(this));
    }
  }

  private void setupHeaderRole(Role role) {
    TextView roleView =
        binding
            .laoNavigationDrawer
            .getHeaderView(0) // We have only one header
            .findViewById(R.id.drawer_header_role);
    roleView.setText(role.getStringId());
  }

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
        getSupportFragmentManager(), R.id.fragment_lao_detail, LaoDetailFragment::newInstance);
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
    startActivity(SocialMediaActivity.newIntent(this, viewModel.getLaoId()));
  }

  public static LaoViewModel obtainViewModel(FragmentActivity activity) {
    return new ViewModelProvider(activity).get(LaoViewModel.class);
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
}
