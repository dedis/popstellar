package com.github.dedis.popstellar.ui.home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.Injection;
import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.ViewModelFactory;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.qrcode.CameraPermissionFragment;
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningFragment;
import com.github.dedis.popstellar.ui.wallet.ContentWalletFragment;
import com.github.dedis.popstellar.ui.wallet.SeedWalletFragment;
import com.github.dedis.popstellar.ui.wallet.WalletFragment;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.util.function.Supplier;

/** HomeActivity represents the entry point for the application. */
public class HomeActivity extends AppCompatActivity {

  private final String TAG = HomeActivity.class.getSimpleName();
  public static final int LAO_DETAIL_REQUEST_CODE = 0;

  private HomeViewModel mViewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.home_activity);

    setupHomeFragment();

    mViewModel = obtainViewModel(this);

    setupHomeButton();
    setupLaunchButton();
    setupConnectButton();
    setupWalletButton();

    // Subscribe to "open lao" event
    mViewModel
        .getOpenLaoEvent()
        .observe(
            this,
            stringEvent -> {
              String laoId = stringEvent.getContentIfNotHandled();
              if (laoId != null) {
                openLaoDetails(laoId);
              }
            });

    // Subscribe to "open home" event
    mViewModel
        .getOpenHomeEvent()
        .observe(
            this,
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setupHomeFragment();
              }
            });

    // Subscribe to "open connecting" event
    mViewModel
        .getOpenConnectingEvent()
        .observe(
            this,
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setupConnectingFragment();
              }
            });

    // Subscribe to "open connect" event
    mViewModel
        .getOpenConnectEvent()
        .observe(
            this,
            stringEvent -> {
              String action = stringEvent.getContentIfNotHandled();
              if (action != null) {
                switch (action) {
                  case HomeViewModel.SCAN:
                    setupScanFragment();
                    break;
                  case HomeViewModel.REQUEST_CAMERA_PERMISSION:
                    setupCameraPermissionFragment();
                    break;
                }
              }
            });

    // Subscribe to "open launch" event
    mViewModel
        .getOpenLaunchEvent()
        .observe(
            this,
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setupLaunchFragment();
              }
            });

    subscribeWalletEvents();
  }

  private void subscribeWalletEvents() {

    // Subscribe to "open Seed" event
    mViewModel
        .getOpenSeedEvent()
        .observe(
            this,
            booleanEvent -> {
              Boolean action = booleanEvent.getContentIfNotHandled();
              if (action != null) {
                setupSeedWalletFragment();
              }
            });

    // Subscribe to "open wallet" event
    mViewModel
        .getOpenWalletEvent()
        .observe(
            this,
            booleanEvent -> {
              Boolean isSetUp = booleanEvent.getContentIfNotHandled();
              if (isSetUp != null) {
                if (isSetUp) {
                  setupContentWalletFragment();
                } else {
                  setupWalletFragment();
                }
              }
            });

    // Subscribe to "open lao wallet" event
    mViewModel
        .getOpenLaoWalletEvent()
        .observe(
            this,
            stringEvent -> {
              String laoId = stringEvent.getContentIfNotHandled();
              if (laoId != null) {
                openContentWallet(laoId);
              }
            });
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == LAO_DETAIL_REQUEST_CODE) {
      if (resultCode == RESULT_OK) {
        startActivity(new Intent(data));
      }
    }
  }

  public static HomeViewModel obtainViewModel(FragmentActivity activity) {
    ViewModelFactory factory = ViewModelFactory.getInstance(activity.getApplication());
    return new ViewModelProvider(activity, factory).get(HomeViewModel.class);
  }

  public void setupHomeButton() {
    Button homeButton = (Button) findViewById(R.id.tab_home);
    homeButton.setOnClickListener(v -> mViewModel.openHome());
  }

  public void setupConnectButton() {
    Button connectButton = (Button) findViewById(R.id.tab_connect);
    connectButton.setOnClickListener(v -> mViewModel.openConnect());
  }

  public void setupLaunchButton() {
    Button launchButton = (Button) findViewById(R.id.tab_launch);
    launchButton.setOnClickListener(v -> mViewModel.openLaunch());
  }

  public void setupWalletButton() {
    Button launchButton = (Button) findViewById(R.id.tab_wallet);
    launchButton.setOnClickListener(v -> mViewModel.openWallet());
  }

  private void setupHomeFragment() {
    setCurrentFragment(R.id.fragment_home, HomeFragment::newInstance);
  }

  private void setupScanFragment() {
    setCurrentFragment(
        R.id.fragment_qrcode,
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
    // Setup result listener to open the connect tab once the permission is granted
    getSupportFragmentManager()
        .setFragmentResultListener(
            CameraPermissionFragment.REQUEST_KEY, this, (k, b) -> mViewModel.openConnect());

    setCurrentFragment(
        R.id.fragment_camera_perm,
        () -> CameraPermissionFragment.newInstance(getActivityResultRegistry()));
  }

  private void setupLaunchFragment() {
    setCurrentFragment(R.id.fragment_launch, LaunchFragment::newInstance);
  }

  private void setupConnectingFragment() {
    setCurrentFragment(R.id.fragment_connecting, ConnectingFragment::newInstance);
  }

  private void setupWalletFragment() {
    setCurrentFragment(R.id.fragment_wallet, WalletFragment::newInstance);
  }

  private void setupContentWalletFragment() {
    setCurrentFragment(R.id.fragment_content_wallet, ContentWalletFragment::newInstance);
  }

  private void setupSeedWalletFragment() {
    setCurrentFragment(R.id.fragment_seed_wallet, SeedWalletFragment::newInstance);
  }

  private void openLaoDetails(String laoId) {
    openLaoDetailActivity(laoId, true);
  }

  private void openContentWallet(String laoId) {
    openLaoDetailActivity(laoId, false);
  }

  private void openLaoDetailActivity(String laoId, boolean openLaoDetail) {
    Intent intent = new Intent(this, LaoDetailActivity.class);
    Log.d(TAG, "Trying to open lao detail for lao with id " + laoId);
    intent.putExtra("LAO_ID", laoId);
    if (openLaoDetail) {
      intent.putExtra("FRAGMENT_TO_OPEN", "LaoDetail");
    } else {
      intent.putExtra("FRAGMENT_TO_OPEN", "ContentWallet");
    }
    startActivityForResult(intent, LAO_DETAIL_REQUEST_CODE);
  }

  /**
   * Set the current fragment in the container of the activity
   *
   * @param id of the fragment
   * @param fragmentSupplier provides the fragment if it is missing
   */
  private void setCurrentFragment(@IdRes int id, Supplier<Fragment> fragmentSupplier) {
    Fragment fragment = getSupportFragmentManager().findFragmentById(id);
    // If the fragment was nt created yet, create it now
    if (fragment == null) fragment = fragmentSupplier.get();

    // Set the new fragment in the container
    ActivityUtils.replaceFragmentInActivity(
        getSupportFragmentManager(), fragment, R.id.fragment_container_home);
  }
}
