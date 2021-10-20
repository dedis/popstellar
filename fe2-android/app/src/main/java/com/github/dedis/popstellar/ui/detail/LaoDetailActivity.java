package com.github.dedis.popstellar.ui.detail;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.Injection;
import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.ViewModelFactory;
import com.github.dedis.popstellar.model.objects.event.EventType;
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

public class LaoDetailActivity extends AppCompatActivity {

  private static final String TAG = LaoDetailActivity.class.getSimpleName();
  private LaoDetailViewModel mViewModel;

  public static LaoDetailViewModel obtainViewModel(FragmentActivity activity) {
    ViewModelFactory factory = ViewModelFactory.getInstance(activity.getApplication());
    LaoDetailViewModel viewModel =
        new ViewModelProvider(activity, factory).get(LaoDetailViewModel.class);
    return viewModel;
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
    LaoDetailFragment laoDetailFragment =
        (LaoDetailFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_lao_detail);
    if (laoDetailFragment == null) {
      laoDetailFragment = LaoDetailFragment.newInstance();
      ActivityUtils.replaceFragmentInActivity(
          getSupportFragmentManager(), laoDetailFragment, R.id.fragment_container_lao_detail);
    }
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
                IdentityFragment identityFragment =
                    (IdentityFragment)
                        getSupportFragmentManager().findFragmentById(R.id.fragment_identity);
                if (identityFragment == null) {
                  identityFragment = IdentityFragment.newInstance(publicKey);
                  ActivityUtils.replaceFragmentInActivity(
                      getSupportFragmentManager(),
                      identityFragment,
                      R.id.fragment_container_lao_detail);
                }
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
                WitnessMessageFragment witnessMessageFragment =
                    (WitnessMessageFragment)
                        getSupportFragmentManager().findFragmentById(R.id.fragment_witness_message);
                if (witnessMessageFragment == null) {
                  witnessMessageFragment = WitnessMessageFragment.newInstance();
                  ActivityUtils.replaceFragmentInActivity(
                      getSupportFragmentManager(),
                      witnessMessageFragment,
                      R.id.fragment_container_lao_detail);
                }
              }
            });
  }

  private void setupCreateRollCallFragment() {
    RollCallEventCreationFragment rollCallCreationFragment =
        (RollCallEventCreationFragment)
            getSupportFragmentManager().findFragmentById(R.id.fragment_create_roll_call_event);
    if (rollCallCreationFragment == null) {
      rollCallCreationFragment = RollCallEventCreationFragment.newInstance();
      ActivityUtils.replaceFragmentInActivity(
          getSupportFragmentManager(),
          rollCallCreationFragment,
          R.id.fragment_container_lao_detail);
    }
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
    QRCodeScanningFragment scanningFragment =
        (QRCodeScanningFragment) getSupportFragmentManager().findFragmentById(R.id.qr_code);

    if (scanningFragment == null) {
      Context context = getApplicationContext();
      BarcodeDetector qrCodeDetector = Injection.provideQRCodeDetector(context);
      int width = getResources().getInteger(R.integer.camera_preview_width);
      int height = getResources().getInteger(R.integer.camera_preview_height);
      scanningFragment =
          QRCodeScanningFragment.newInstance(
              Injection.provideCameraSource(context, qrCodeDetector, width, height),
              qrCodeDetector);
      ActivityUtils.replaceFragmentInActivity(
          getSupportFragmentManager(), scanningFragment, R.id.fragment_container_lao_detail);
    }
  }

  private void setupScanFragmentRollCall() {
    QRCodeScanningFragment scanningFragment =
        (QRCodeScanningFragment)
            getSupportFragmentManager().findFragmentById(R.id.add_attendee_layout);

    if (scanningFragment == null) {
      Context context = getApplicationContext();
      BarcodeDetector qrCodeDetector = Injection.provideQRCodeDetector(context);
      int width = getResources().getInteger(R.integer.camera_preview_width);
      int height = getResources().getInteger(R.integer.camera_preview_height);
      scanningFragment =
          QRCodeScanningFragment.newInstance(
              Injection.provideCameraSource(context, qrCodeDetector, width, height),
              qrCodeDetector);
      ActivityUtils.replaceFragmentInActivity(
          getSupportFragmentManager(), scanningFragment, R.id.fragment_container_lao_detail);
    }
  }

  private void setupCameraPermissionFragment() {
    CameraPermissionFragment cameraPermissionFragment =
        (CameraPermissionFragment)
            getSupportFragmentManager().findFragmentById(R.id.fragment_camera_perm);
    if (cameraPermissionFragment == null) {
      cameraPermissionFragment = CameraPermissionFragment.newInstance();
      ActivityUtils.replaceFragmentInActivity(
          getSupportFragmentManager(),
          cameraPermissionFragment,
          R.id.fragment_container_lao_detail);
    }
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
    RollCallDetailFragment rollCallDetailFragment =
        (RollCallDetailFragment)
            getSupportFragmentManager().findFragmentById(R.id.fragment_roll_call_detail);
    if (rollCallDetailFragment == null) {
      rollCallDetailFragment = RollCallDetailFragment.newInstance(pk);
      ActivityUtils.replaceFragmentInActivity(
          getSupportFragmentManager(), rollCallDetailFragment, R.id.fragment_container_lao_detail);
    }
  }

  private void setupCreateElectionSetupFragment() {
    ElectionSetupFragment electionSetupFragment =
        (ElectionSetupFragment)
            getSupportFragmentManager().findFragmentById(R.id.fragment_setup_election_event);
    if (electionSetupFragment == null) {
      electionSetupFragment = ElectionSetupFragment.newInstance();
      ActivityUtils.replaceFragmentInActivity(
          getSupportFragmentManager(), electionSetupFragment, R.id.fragment_container_lao_detail);
    }
  }

  private void setupLaoWalletFragment() {
    LaoWalletFragment laoWalletFragment =
        (LaoWalletFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_lao_wallet);
    if (laoWalletFragment == null) {
      laoWalletFragment = LaoWalletFragment.newInstance();
      ActivityUtils.replaceFragmentInActivity(
          getSupportFragmentManager(), laoWalletFragment, R.id.fragment_container_lao_detail);
    }
  }

  private void setupRollCallTokenFragment(String id) {
    RollCallTokenFragment rollCallTokenFragment =
        (RollCallTokenFragment)
            getSupportFragmentManager().findFragmentById(R.id.fragment_rollcall_token);
    if (rollCallTokenFragment == null) {
      rollCallTokenFragment = RollCallTokenFragment.newInstance(id);
      ActivityUtils.replaceFragmentInActivity(
          getSupportFragmentManager(), rollCallTokenFragment, R.id.fragment_container_lao_detail);
    }
  }

  private void setupAttendeesListFragment(String id) {
    AttendeesListFragment attendeesListFragment =
        (AttendeesListFragment)
            getSupportFragmentManager().findFragmentById(R.id.fragment_attendees_list);
    if (attendeesListFragment == null) {
      attendeesListFragment = AttendeesListFragment.newInstance(id);
      ActivityUtils.replaceFragmentInActivity(
          getSupportFragmentManager(), attendeesListFragment, R.id.fragment_container_lao_detail);
    }
  }

  private void setupManageElectionFragment() {

    mViewModel
        .getOpenManageElectionEvent()
        .observe(
            this,
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                ManageElectionFragment manageElectionFragment =
                    (ManageElectionFragment)
                        getSupportFragmentManager().findFragmentById(R.id.fragment_manage_election);
                if (manageElectionFragment == null) {
                  manageElectionFragment = ManageElectionFragment.newInstance();
                  ActivityUtils.replaceFragmentInActivity(
                      getSupportFragmentManager(),
                      manageElectionFragment,
                      R.id.fragment_container_lao_detail);
                }
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
                CastVoteFragment castVoteFragment =
                    (CastVoteFragment)
                        getSupportFragmentManager().findFragmentById(R.id.fragment_cast_vote);
                if (castVoteFragment == null) {
                  castVoteFragment = CastVoteFragment.newInstance();
                  ActivityUtils.replaceFragmentInActivity(
                      getSupportFragmentManager(),
                      castVoteFragment,
                      R.id.fragment_container_lao_detail);
                }
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
                ElectionResultFragment electionResultFragment =
                    (ElectionResultFragment)
                        getSupportFragmentManager().findFragmentById(R.id.fragment_election_result);
                if (electionResultFragment == null) {
                  electionResultFragment = ElectionResultFragment.newInstance();
                  ActivityUtils.replaceFragmentInActivity(
                      getSupportFragmentManager(),
                      electionResultFragment,
                      R.id.fragment_container_lao_detail);
                }
              }
            });
  }
}
