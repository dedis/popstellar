package com.github.dedis.popstellar.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.*;
import androidx.appcompat.app.*;
import androidx.fragment.app.*;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.event.EventType;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.ui.detail.event.consensus.ElectionStartFragment;
import com.github.dedis.popstellar.ui.detail.event.election.fragments.*;
import com.github.dedis.popstellar.ui.detail.event.rollcall.*;
import com.github.dedis.popstellar.ui.detail.witness.WitnessMessageFragment;
import com.github.dedis.popstellar.ui.detail.witness.WitnessingFragment;
import com.github.dedis.popstellar.ui.digitalcash.DigitalCashActivity;
import com.github.dedis.popstellar.ui.home.HomeActivity;
import com.github.dedis.popstellar.ui.home.HomeViewModel;
import com.github.dedis.popstellar.ui.qrcode.*;
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
      setupLaoFragment();
    } else {
      setupLaoWalletFragment();
    }

    // Subscribe to "open lao detail event"
    mViewModel
        .getOpenLaoDetailEvent()
        .observe(
            this,
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setupLaoFragment();
              }
            });
    // Subscribe to "open home" event
    setupHomeActivity();

    // Subscribe to "open identity" event
    setupIdentityFragment();

    // Subscribe to " open social media " event
    setupSocialMediaActivity();

    // Subscribe to " open witness message" event
    setupWitnessMessageFragment();

    // Subscribe to "open witnessing" event
    setupWitnessing();

    // Subscribe to "new lao event" event
    handleNewEvent();

    // Subscribe to "open roll call" event
    mViewModel
        .getOpenRollCallEvent()
        .observe(
            this,
            stringEvent -> {
              HomeViewModel.HomeViewAction action = stringEvent.getContentIfNotHandled();
              if (action != null) {
                openScanning(action);
              }
            });

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
                setupLaoFragment();
              }
            });

    subscribeWalletEvents();

    // Subscribe to "open cast votes event" event
    setupCastVotesFragment();

    // Subscribe to "open election display" event
    setupElectionResultsFragment();

    // Subscribe to "open manage election" event
    setupElectionFragment();

    // Subscribe to "open start election" event
    setupElectionStartFragment();

    mViewModel
        .getOpenDigitalCashEvent()
        .observe(
            this,
            booleanSingleEvent -> {
              Boolean event = booleanSingleEvent.getContentIfNotHandled();
              if (event != null) {
                openDigitalCash();
              }
            });
  }

  private void openDigitalCash() {
    Intent intent = new Intent(this, DigitalCashActivity.class);

    intent.putExtra(Constants.LAO_ID_EXTRA, mViewModel.getCurrentLaoValue().getId());
    intent.putExtra(Constants.LAO_NAME, mViewModel.getCurrentLaoValue().getName());
    startActivity(intent);
  }

  private void setupWitnessing() {
    mViewModel
        .getOpenWitnessing()
        .observe(
            this,
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setupWitnessingFragment();
              }
            });
  }

  private void subscribeWalletEvents() {
    mViewModel
        .getOpenLaoWalletEvent()
        .observe(
            this,
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setupLaoWalletFragment();
              }
            });
    mViewModel
        .getOpenRollCallTokenEvent()
        .observe(
            this,
            stringEvent -> {
              String id = stringEvent.getContentIfNotHandled();
              if (id != null) {
                setupRollCallTokenFragment(id);
              }
            });
    mViewModel
        .getOpenAttendeesListEvent()
        .observe(
            this,
            stringEvent -> {
              String id = stringEvent.getContentIfNotHandled();
              if (id != null) {
                setupAttendeesListFragment(id);
              }
            });
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

  public void handleNewEvent() {
    mViewModel
        .getNewLaoEventEvent()
        .observe(
            this,
            eventEvent -> {
              EventType eventType = eventEvent.getContentIfNotHandled();
              if (eventType != null) {
                Log.d(TAG, "event type: " + eventType);
                switch (eventType) {
                  case ROLL_CALL:
                    setupCreateRollCallFragment();
                    break;
                  case ELECTION:
                    setupCreateElectionSetupFragment();
                    break;
                  default:
                    Log.d(TAG, "unknown event type: " + eventType);
                }
              }
            });
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
    if (menuItem.getItemId() == android.R.id.home) {
      Fragment fragment =
          getSupportFragmentManager().findFragmentById(R.id.fragment_container_lao_detail);
      if (fragment instanceof LaoDetailFragment) {
        openHome();
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

  private void setupLaoFragment() {
    setCurrentFragment(
        getSupportFragmentManager(), R.id.fragment_lao_detail, LaoDetailFragment::newInstance);
  }

  private void setupHomeActivity() {
    mViewModel
        .getOpenHomeEvent()
        .observe(
            this,
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                openHome();
              }
            });
  }

  private void openHome() {
    Intent intent = new Intent(this, HomeActivity.class);
    setResult(HomeActivity.LAO_DETAIL_REQUEST_CODE, intent);
    startActivity(intent);
    finish();
  }

  private void setupIdentityFragment() {
    mViewModel
        .getOpenIdentityEvent()
        .observe(
            this,
            stringEvent -> {
              PublicKey publicKey = stringEvent.getContentIfNotHandled();
              if (publicKey != null) {
                setCurrentFragment(
                    getSupportFragmentManager(),
                    R.id.fragment_identity,
                    () -> IdentityFragment.newInstance(publicKey));
              }
            });
  }

  private void setupSocialMediaActivity() {
    mViewModel
        .getOpenSocialMediaEvent()
        .observe(
            this,
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                startActivity(
                    SocialMediaActivity.newInstance(
                        this,
                        mViewModel.getCurrentLaoValue().getId(),
                        mViewModel.getCurrentLaoValue().getName()));
              }
            });
  }

  private void setupWitnessMessageFragment() {
    mViewModel
        .getOpenWitnessMessageEvent()
        .observe(
            this,
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setCurrentFragment(
                    getSupportFragmentManager(),
                    R.id.fragment_witness_message,
                    WitnessMessageFragment::newInstance);
              }
            });
  }

  private void setupCreateRollCallFragment() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_create_roll_call_event,
        RollCallCreationFragment::newInstance);
  }

  private void setupScanFragmentWitness() {
    setCurrentFragment(getSupportFragmentManager(), R.id.qr_code, QRCodeScanningFragment::new);
  }

  private void setupScanFragmentRollCall() {
    setCurrentFragment(
        getSupportFragmentManager(), R.id.add_attendee_layout, QRCodeScanningFragment::new);
  }

  private void setupCameraPermissionFragment() {
    // Setup result listener to open the scanning tab once the permission is granted
    getSupportFragmentManager()
        .setFragmentResultListener(
            CameraPermissionFragment.REQUEST_KEY, this, (k, b) -> mViewModel.openScanning());

    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_camera_perm,
        () -> CameraPermissionFragment.newInstance(getActivityResultRegistry()));
  }

  private void openScanning(HomeViewModel.HomeViewAction action) {
    if (action.equals(HomeViewModel.HomeViewAction.SCAN)) {
      if (mViewModel.getScanningAction() == ScanningAction.ADD_ROLL_CALL_ATTENDEE) {
        setupScanFragmentRollCall();
      } else {
        setupScanFragmentWitness();
      }
    } else {
      setupCameraPermissionFragment();
    }
  }

  private void setupCreateElectionSetupFragment() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_setup_election_event,
        ElectionSetupFragment::newInstance);
  }

  private void setupLaoWalletFragment() {
    setCurrentFragment(
        getSupportFragmentManager(), R.id.fragment_lao_wallet, LaoWalletFragment::newInstance);
  }

  private void setupWitnessingFragment() {
    setCurrentFragment(
        getSupportFragmentManager(), R.id.fragment_witnessing, WitnessingFragment::newInstance);
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

  private void setupElectionFragment() {
    mViewModel
        .getOpenElectionFragmentEvent()
        .observe(
            this,
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                Log.d(TAG, "opening new election fragment");
                setCurrentFragment(
                    getSupportFragmentManager(),
                    R.id.fragment_election,
                    ElectionFragment::newInstance);
              }
            });
  }

  public void setUpWalletMessage() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("You have to setup up your wallet before connecting.");
    builder.setPositiveButton("Ok", (dialog, which) -> dialog.dismiss());
    builder.show();
  }

  private void setupCastVotesFragment() {
    mViewModel
        .getOpenCastVotes()
        .observe(
            this,
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setCurrentFragment(
                    getSupportFragmentManager(),
                    R.id.fragment_cast_vote,
                    CastVoteFragment::newInstance);
              }
            });
  }

  private void setupElectionResultsFragment() {
    mViewModel
        .getOpenElectionResultsEvent()
        .observe(
            this,
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setCurrentFragment(
                    getSupportFragmentManager(),
                    R.id.fragment_election_result,
                    ElectionResultFragment::newInstance);
              }
            });
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
            setupLaoFragment();
          } else if (id == R.id.lao_detail_identity_menu) {
            mViewModel.openIdentity();
          } else if (id == R.id.lao_detail_witnessing_menu) {
            mViewModel.openWitnessing();
          } else if (id == R.id.lao_detail_digital_cash_menu) {
            mViewModel.openDigitalCash();
          } else if (id == R.id.lao_detail_social_media_menu) {
            mViewModel.openSocialMedia();
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
