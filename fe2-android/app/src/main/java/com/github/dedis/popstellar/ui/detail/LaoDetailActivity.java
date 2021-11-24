package com.github.dedis.popstellar.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.Injection;
import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.ViewModelFactory;
import com.github.dedis.popstellar.model.objects.event.EventType;
import com.github.dedis.popstellar.ui.detail.event.consensus.ElectionStartFragment;
import com.github.dedis.popstellar.ui.detail.event.election.fragments.CastVoteFragment;
import com.github.dedis.popstellar.ui.detail.event.election.fragments.ElectionResultFragment;
import com.github.dedis.popstellar.ui.detail.event.election.fragments.ElectionSetupFragment;
import com.github.dedis.popstellar.ui.detail.event.election.fragments.ManageElectionFragment;
import com.github.dedis.popstellar.ui.detail.event.rollcall.AttendeesListFragment;
import com.github.dedis.popstellar.ui.detail.event.rollcall.RollCallDetailFragment;
import com.github.dedis.popstellar.ui.detail.event.rollcall.RollCallEventCreationFragment;
import com.github.dedis.popstellar.ui.detail.event.rollcall.RollCallTokenFragment;
import com.github.dedis.popstellar.ui.detail.witness.WitnessMessageFragment;
import com.github.dedis.popstellar.ui.home.HomeActivity;
import com.github.dedis.popstellar.ui.home.HomeViewModel;
import com.github.dedis.popstellar.ui.qrcode.CameraPermissionFragment;
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningFragment;
import com.github.dedis.popstellar.ui.qrcode.ScanningAction;
import com.github.dedis.popstellar.ui.wallet.LaoWalletFragment;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.util.Objects;
import java.util.function.Supplier;

public class LaoDetailActivity extends AppCompatActivity {

  private static final String TAG = LaoDetailActivity.class.getSimpleName();
  private LaoDetailViewModel mViewModel;

  public static LaoDetailViewModel obtainViewModel(FragmentActivity activity) {
    ViewModelFactory factory = Injection.provideViewModelFactory(activity.getApplication());
    return new ViewModelProvider(activity, factory).get(LaoDetailViewModel.class);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.lao_detail_activity);
    mViewModel = obtainViewModel(this);
    mViewModel.subscribeToLao(
        (String) Objects.requireNonNull(getIntent().getExtras()).get("LAO_ID"));
    if (getIntent().getExtras().get("FRAGMENT_TO_OPEN").equals("LaoDetail")) {
      setupLaoFragment();
    } else {
      setupLaoWalletFragment();
    }
    setupHomeButton();
    setupIdentityButton();
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
    // Subscribe to " open witness message" event
    setupWitnessMessageFragment();
    // Subscribe to "add witness" event
    setupAddWitness();
    // Subscribe to "new lao event" event
    handleNewEvent();

    // Subscribe to "open roll call" event
    mViewModel
        .getOpenRollCallEvent()
        .observe(
            this,
            stringEvent -> {
              String action = stringEvent.getContentIfNotHandled();
              if (action != null) {
                openScanning(action);
              }
            });
    mViewModel
        .getCloseRollCallEvent()
        .observe(
            this,
            integerEvent -> {
              Integer nextFragment = integerEvent.getContentIfNotHandled();
              if (nextFragment != null) {
                if (nextFragment.equals(R.id.fragment_lao_detail)) {
                  mViewModel.openLaoDetail();
                } else if (nextFragment.equals(R.id.fragment_home)) {
                  mViewModel.openHome();
                } else if (nextFragment.equals(R.id.fragment_identity)) {
                  mViewModel.openIdentity();
                }
              }
            });
    mViewModel
        .getPkRollCallEvent()
        .observe(
            this,
            stringEvent -> {
              String pk = stringEvent.getContentIfNotHandled();
              if (pk != null) {
                setupRollCallDetailFragment(pk);
              }
            });
    subscribeWalletEvents();

    // Subscribe to "open cast votes event" event
    setupCastVotesFragment();

    // Subscribe to "open election display" event
    setupElectionResultsFragment();

    // Subscribe to "open manage election" event
    setupManageElectionFragment();

    // Subscribe to "open start election" event
    setupElectionStartFragment();
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
                Log.d(TAG, "event type: " + eventType.toString());
                switch (eventType) {
                  case ROLL_CALL:
                    setupCreateRollCallFragment();
                    break;
                  case ELECTION:
                    setupCreateElectionSetupFragment();
                    break;
                  default:
                    Log.d(TAG, "unknown event type: " + eventType.toString());
                }
              }
            });
  }

  public void setupHomeButton() {
    Button homeButton = (Button) findViewById(R.id.tab_home);
    homeButton.setOnClickListener(v -> mViewModel.openHome());
  }

  public void setupIdentityButton() {
    Button identityButton = (Button) findViewById(R.id.tab_identity);
    identityButton.setOnClickListener(v -> mViewModel.openIdentity());
  }

  private void setupLaoFragment() {
    setCurrentFragment(R.id.fragment_lao_detail, LaoDetailFragment::newInstance);
  }

  private void setupHomeActivity() {
    mViewModel
        .getOpenHomeEvent()
        .observe(
            this,
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                Intent intent = new Intent(this, HomeActivity.class);
                setResult(HomeActivity.LAO_DETAIL_REQUEST_CODE, intent);
                finish();
              }
            });
  }

  private void setupIdentityFragment() {
    mViewModel
        .getOpenIdentityEvent()
        .observe(
            this,
            stringEvent -> {
              String publicKey = stringEvent.getContentIfNotHandled();
              if (publicKey != null) {
                setCurrentFragment(
                    R.id.fragment_identity, () -> IdentityFragment.newInstance(publicKey));
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
                    R.id.fragment_witness_message, WitnessMessageFragment::newInstance);
              }
            });
  }

  private void setupCreateRollCallFragment() {
    setCurrentFragment(
        R.id.fragment_create_roll_call_event, RollCallEventCreationFragment::newInstance);
  }

  private void setupAddWitness() {

    // Subscribe to "open witness " event
    mViewModel
        .getOpenAddWitness()
        .observe(
            this,
            stringEvent -> {
              String action = stringEvent.getContentIfNotHandled();
              if (action != null) {
                openScanning(action);
              }
            });
  }

  private void setupScanFragmentWitness() {
    setCurrentFragment(
        R.id.qr_code,
        () -> {
          Context context = getApplicationContext();
          BarcodeDetector qrCodeDetector = Injection.provideQRCodeDetector(context);
          return QRCodeScanningFragment.newInstance(
              Injection.provideCameraSource(
                  getApplicationContext(),
                  qrCodeDetector,
                  getResources().getInteger(R.integer.camera_preview_width),
                  getResources().getInteger(R.integer.camera_preview_height)),
              qrCodeDetector);
        });
  }

  private void setupScanFragmentRollCall() {
    setCurrentFragment(
        R.id.add_attendee_layout,
        () -> {
          Context context = getApplicationContext();
          BarcodeDetector qrCodeDetector = Injection.provideQRCodeDetector(context);
          return QRCodeScanningFragment.newInstance(
              Injection.provideCameraSource(
                  getApplicationContext(),
                  qrCodeDetector,
                  getResources().getInteger(R.integer.camera_preview_width),
                  getResources().getInteger(R.integer.camera_preview_height)),
              qrCodeDetector);
        });
  }

  private void setupCameraPermissionFragment() {
    // Setup result listener to open the scanning tab once the permission is granted
    getSupportFragmentManager()
        .setFragmentResultListener(
            CameraPermissionFragment.REQUEST_KEY, this, (k, b) -> mViewModel.openScanning());

    setCurrentFragment(
        R.id.fragment_camera_perm,
        () -> CameraPermissionFragment.newInstance(getActivityResultRegistry()));
  }

  private void openScanning(String action) {
    if (action.equals(HomeViewModel.SCAN)) {
      if (mViewModel.getScanningAction() == ScanningAction.ADD_ROLL_CALL_ATTENDEE) {
        setupScanFragmentRollCall();
      } else {
        setupScanFragmentWitness();
      }
    } else {
      setupCameraPermissionFragment();
    }
  }

  private void setupRollCallDetailFragment(String pk) {
    setCurrentFragment(
        R.id.fragment_roll_call_detail, () -> RollCallDetailFragment.newInstance(pk));
  }

  private void setupCreateElectionSetupFragment() {
    setCurrentFragment(R.id.fragment_setup_election_event, ElectionSetupFragment::newInstance);
  }

  private void setupLaoWalletFragment() {
    setCurrentFragment(R.id.fragment_lao_wallet, LaoWalletFragment::newInstance);
  }

  private void setupRollCallTokenFragment(String id) {
    setCurrentFragment(R.id.fragment_rollcall_token, () -> RollCallTokenFragment.newInstance(id));
  }

  private void setupAttendeesListFragment(String id) {
    setCurrentFragment(R.id.fragment_attendees_list, () -> AttendeesListFragment.newInstance(id));
  }

  private void setupManageElectionFragment() {
    mViewModel
        .getOpenManageElectionEvent()
        .observe(
            this,
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setCurrentFragment(
                    R.id.fragment_manage_election, ManageElectionFragment::newInstance);
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
                setCurrentFragment(R.id.fragment_cast_vote, CastVoteFragment::newInstance);
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
                    R.id.fragment_election_result, ElectionResultFragment::newInstance);
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
                    R.id.fragment_election_start, ElectionStartFragment::newInstance);
              }
            });
  }

  /**
   * Set the current fragment in the container of the activity
   *
   * @param id of the fragment
   * @param fragmentSupplier provides the fragment if it is missing
   */
  private void setCurrentFragment(@IdRes int id, Supplier<Fragment> fragmentSupplier) {
    Fragment fragment = getSupportFragmentManager().findFragmentById(id);
    // If the fragment was not created yet, create it now
    if (fragment == null) fragment = fragmentSupplier.get();

    // Set the new fragment in the container
    ActivityUtils.replaceFragmentInActivity(
        getSupportFragmentManager(), fragment, R.id.fragment_container_lao_detail);
  }
}
