package com.github.dedis.popstellar.ui.lao;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.*;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.LaoActivityBinding;
import com.github.dedis.popstellar.model.Role;
import com.github.dedis.popstellar.ui.home.HomeActivity;
import com.github.dedis.popstellar.ui.lao.digitalcash.*;
import com.github.dedis.popstellar.ui.lao.event.EventsViewModel;
import com.github.dedis.popstellar.ui.lao.event.consensus.ConsensusViewModel;
import com.github.dedis.popstellar.ui.lao.event.election.ElectionViewModel;
import com.github.dedis.popstellar.ui.lao.event.eventlist.EventListFragment;
import com.github.dedis.popstellar.ui.lao.event.rollcall.RollCallViewModel;
import com.github.dedis.popstellar.ui.lao.socialmedia.SocialMediaHomeFragment;
import com.github.dedis.popstellar.ui.lao.socialmedia.SocialMediaViewModel;
import com.github.dedis.popstellar.ui.lao.token.TokenListFragment;
import com.github.dedis.popstellar.ui.lao.witness.WitnessingFragment;
import com.github.dedis.popstellar.ui.lao.witness.WitnessingViewModel;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.Constants;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.GeneralSecurityException;
import java.util.*;
import java.util.function.Supplier;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LaoActivity extends AppCompatActivity {
  private static final Logger logger = LogManager.getLogger(LaoActivity.class);

  LaoViewModel laoViewModel;
  LaoActivityBinding binding;
  private final Deque<Fragment> fragmentStack = new LinkedList<>();

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = LaoActivityBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    laoViewModel = obtainViewModel(this);

    String laoId =
        Objects.requireNonNull(getIntent().getExtras()).getString(Constants.LAO_ID_EXTRA);
    laoViewModel.setLaoId(laoId);

    laoViewModel.observeLao(laoId);
    laoViewModel.observeRollCalls(laoId);

    observeRoles();
    observeToolBar();
    observeDrawer();
    setupDrawerHeader();

    // Open Event list on activity creation
    setEventsTab();
  }

  @Override
  /*
   Normally the saving routine should be called onStop, such as is done in other activities,
   Yet here for unknown reasons the subscriptions set in LAONetworkManager is empty when going
   to HomeActivity. This fixes it. Since our persistence is light for now (13.02.2023) - i.e.
   server address, wallet seed and channel list - and not computationally intensive this will not
   be a problem at the moment
  */
  public void onPause() {
    super.onPause();

    try {
      laoViewModel.savePersistentData();
    } catch (GeneralSecurityException e) {
      // We do not display the security error to the user
      logger.debug("Storage was unsuccessful du to wallet error " + e);
      Toast.makeText(this, R.string.error_storage_wallet, Toast.LENGTH_SHORT).show();
    }
  }

  private void observeRoles() {
    // Observe any change in the following variable to update the role
    laoViewModel.isWitness().observe(this, any -> laoViewModel.updateRole());
    laoViewModel.isAttendee().observe(this, any -> laoViewModel.updateRole());

    // Update the user's role in the drawer header when it changes
    laoViewModel.getRole().observe(this, this::setupHeaderRole);
  }

  private void observeToolBar() {
    // Listen to click on left icon of toolbar
    binding.laoAppBar.setNavigationOnClickListener(
        view -> {
          if (Boolean.TRUE.equals(laoViewModel.isTab().getValue())) {
            // If it is a tab open menu
            binding.laoDrawerLayout.openDrawer(GravityCompat.START);
          } else {
            // Press back arrow
            onBackPressed();
          }
        });

    // Observe whether the menu icon or back arrow should be displayed
    laoViewModel
        .isTab()
        .observe(
            this,
            isTab ->
                binding.laoAppBar.setNavigationIcon(
                    Boolean.TRUE.equals(isTab)
                        ? R.drawable.menu_icon
                        : R.drawable.back_arrow_icon));

    // Observe the toolbar title to display
    laoViewModel
        .getPageTitle()
        .observe(
            this,
            resId -> {
              if (resId != 0) {
                binding.laoAppBar.setTitle(resId);
              }
            });

    // Listener for the transaction button
    binding.laoAppBar.setOnMenuItemClickListener(
        menuItem -> {
          if (menuItem.getItemId() == R.id.history_menu_toolbar) {
            // If the user clicks on the button when the transaction history is
            // already displayed, then consider it as a back button pressed
            Fragment fragment =
                getSupportFragmentManager().findFragmentById(R.id.fragment_container_lao);
            if (!(fragment instanceof DigitalCashHistoryFragment)) {
              // Push onto the stack the current fragment to restore it upon exit
              fragmentStack.push(fragment);
              setCurrentFragment(
                  getSupportFragmentManager(),
                  R.id.fragment_digital_cash_history,
                  DigitalCashHistoryFragment::newInstance);
            } else {
              // Restore the fragment pushed on the stack before opening the transaction history
              resetLastFragment();
            }
            return true;
          }
          return false;
        });
  }

  private void observeDrawer() {
    // Observe changes to the tab selected
    laoViewModel
        .getCurrentTab()
        .observe(
            this,
            tab -> {
              laoViewModel.setIsTab(true);
              binding.laoNavigationDrawer.setCheckedItem(tab.getMenuId());
            });

    binding.laoNavigationDrawer.setNavigationItemSelectedListener(
        item -> {
          MainMenuTab tab = MainMenuTab.findByMenu(item.getItemId());
          logger.info("Opening tab : " + tab.getName());
          boolean selected = openTab(tab);
          if (selected) {
            logger.debug("The tab was successfully opened");
            laoViewModel.setCurrentTab(tab);
          } else {
            logger.debug("The tab wasn't opened");
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
      laoNameView.setText(laoViewModel.getLao().getName());
    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(this, logger, e, R.string.unknown_lao_exception);
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
        return true;
      case SOCIAL_MEDIA:
        openSocialMediaTab();
        return true;
      case DISCONNECT:
        startActivity(HomeActivity.newIntent(this));
        return true;
      default:
        logger.warn("Unhandled tab type : " + tab);
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
    setCurrentFragment(
        getSupportFragmentManager(), R.id.fragment_digital_cash_home, DigitalCashHomeFragment::new);
  }

  private void openSocialMediaTab() {
    setCurrentFragment(
        getSupportFragmentManager(), R.id.fragment_social_media_home, SocialMediaHomeFragment::new);
  }

  /** Open Event list and select item in drawer menu */
  private void setEventsTab() {
    binding.laoNavigationDrawer.setCheckedItem(MainMenuTab.EVENTS.getMenuId());
    openEventsTab();
  }

  /** Restore the fragment contained in the stack as container of the current lao */
  public void resetLastFragment() {
    Fragment fragment = fragmentStack.pop();
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.fragment_container_lao, fragment)
        .commit();
  }

  public static LaoViewModel obtainViewModel(FragmentActivity activity) {
    return new ViewModelProvider(activity).get(LaoViewModel.class);
  }

  public static EventsViewModel obtainEventsEventsViewModel(
      FragmentActivity activity, String laoId) {
    EventsViewModel eventsViewModel = new ViewModelProvider(activity).get(EventsViewModel.class);
    eventsViewModel.setId(laoId);
    return eventsViewModel;
  }

  public static ConsensusViewModel obtainConsensusViewModel(
      FragmentActivity activity, String laoId) {

    ConsensusViewModel consensusViewModel =
        new ViewModelProvider(activity).get(ConsensusViewModel.class);
    consensusViewModel.setLaoId(laoId);
    return consensusViewModel;
  }

  public static ElectionViewModel obtainElectionViewModel(FragmentActivity activity, String laoId) {
    ElectionViewModel electionViewModel =
        new ViewModelProvider(activity).get(ElectionViewModel.class);
    electionViewModel.setLaoId(laoId);
    return electionViewModel;
  }

  public static RollCallViewModel obtainRollCallViewModel(FragmentActivity activity, String laoId) {
    RollCallViewModel rollCallViewModel =
        new ViewModelProvider(activity).get(RollCallViewModel.class);
    rollCallViewModel.setLaoId(laoId);
    return rollCallViewModel;
  }

  public static WitnessingViewModel obtainWitnessingViewModel(
      FragmentActivity activity, String laoId) {
    WitnessingViewModel witnessingViewModel =
        new ViewModelProvider(activity).get(WitnessingViewModel.class);
    witnessingViewModel.setLaoId(laoId);
    return witnessingViewModel;
  }

  public static SocialMediaViewModel obtainSocialMediaViewModel(
      FragmentActivity activity, String laoId) {
    SocialMediaViewModel socialMediaViewModel =
        new ViewModelProvider(activity).get(SocialMediaViewModel.class);
    socialMediaViewModel.setLaoId(laoId);
    return socialMediaViewModel;
  }

  public static DigitalCashViewModel obtainDigitalCashViewModel(
      FragmentActivity activity, String laoId) {
    DigitalCashViewModel digitalCashViewModel =
        new ViewModelProvider(activity).get(DigitalCashViewModel.class);
    digitalCashViewModel.setLaoId(laoId);
    return digitalCashViewModel;
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
        manager, R.id.fragment_container_lao, id, fragmentSupplier);
  }

  public static Intent newIntentForLao(Context ctx, String laoId) {
    Intent intent = new Intent(ctx, LaoActivity.class);
    intent.putExtra(Constants.LAO_ID_EXTRA, laoId);
    return intent;
  }

  /** Adds a callback that describes the action to take the next time the back button is pressed */
  public static void addBackNavigationCallback(
      FragmentActivity activity, LifecycleOwner lifecycleOwner, OnBackPressedCallback callback) {
    activity.getOnBackPressedDispatcher().addCallback(lifecycleOwner, callback);
  }

  /** Adds a specific callback for the back button that opens the events tab */
  public static void addBackNavigationCallbackToEvents(
      FragmentActivity activity, LifecycleOwner lifecycleOwner, Logger logger) {
    addBackNavigationCallback(
        activity,
        lifecycleOwner,
        ActivityUtils.buildBackButtonCallback(
            logger, "event list", ((LaoActivity) activity)::setEventsTab));
  }
}
