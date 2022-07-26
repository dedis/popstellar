package com.github.dedis.popstellar.ui.home;

import android.content.Context;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.*;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.network.serializer.JsonUtils;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.home.connecting.ConnectingActivity;
import com.github.dedis.popstellar.ui.qrcode.CameraPermissionFragment;
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningFragment;
import com.github.dedis.popstellar.ui.settings.SettingsActivity;
import com.github.dedis.popstellar.ui.socialmedia.SocialMediaActivity;
import com.github.dedis.popstellar.ui.wallet.*;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.Constants;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.NoLAOException;
import com.github.dedis.popstellar.utility.error.keys.UninitializedWalletException;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.function.Supplier;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.fragment.app.*;
import androidx.fragment.app.FragmentActivity;
import dagger.hilt.android.AndroidEntryPoint;

import static com.github.dedis.popstellar.ui.home.HomeViewModel.setCurrentFragment;

/** HomeActivity represents the entry point for the application. */
@AndroidEntryPoint
public class HomeActivity extends AppCompatActivity {

  private final String TAG = HomeActivity.class.getSimpleName();
  public static final int LAO_DETAIL_REQUEST_CODE = 0;

  private static final int CONNECT_POSITION = 1;
  private static final int LAUNCH_POSITION = 2;
  private static final int SOCIAL_MEDIA_POSITION = 4;

  private HomeViewModel mViewModel;

  private BottomNavigationView navbar;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.home_activity);

    setCurrentFragment(getSupportFragmentManager(), R.id.fragment_home, HomeFragment::newInstance);

    mViewModel = obtainViewModel(this);

    // Load all the json schemas in background when the app is started.
    AsyncTask.execute(
        () -> {
          JsonUtils.loadSchema(JsonUtils.ROOT_SCHEMA);
          JsonUtils.loadSchema(JsonUtils.DATA_SCHEMA);
          JsonUtils.loadSchema(JsonUtils.GENERAL_MESSAGE_SCHEMA);
        });

    navbar = findViewById(R.id.home_nav_bar);
    setupNavigationBar();

    subscribeWalletEvents();
    subscribeSocialMediaEvents();
  }

  private void subscribeWalletEvents() {

    MenuItem connectItem = navbar.getMenu().getItem(CONNECT_POSITION);
    MenuItem launchItem = navbar.getMenu().getItem(LAUNCH_POSITION);

    mViewModel
        .getIsWalletSetUpEvent()
        .observe(
            this,
            aBoolean -> {
              // We set transparency of the
              if (Boolean.TRUE.equals(aBoolean)) {
                connectItem.setIcon(R.drawable.ic_home_connect_opaque_foreground);
                launchItem.setIcon(R.drawable.ic_home_launch_opaque_foreground);
              } else {
                connectItem.setIcon(R.drawable.ic_home_connect_transparent_foreground);
                launchItem.setIcon(R.drawable.ic_home_launch_transparent_foreground);
              }
            });
  }

  @Override
  protected void onResume() {
    super.onResume();
    mViewModel.openHome(getSupportFragmentManager());
  }

  private void subscribeSocialMediaEvents() {
    MenuItem socialMediaItem = navbar.getMenu().getItem(SOCIAL_MEDIA_POSITION);

    // Subscribe to lao adding event to adapt the social media menu item
    mViewModel
        .getLAOs()
        .observe(
            this,
            laos -> {
              if (laos.isEmpty()) {
                socialMediaItem.setIcon(R.drawable.ic_common_social_media_transparent_foreground);
              } else {
                socialMediaItem.setIcon(R.drawable.ic_common_social_media_opaque_foreground);
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

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.options_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.settings) {
      Intent intent = new Intent(this, SettingsActivity.class);
      Log.d(HomeViewModel.TAG, "Trying to open settings");
      startActivity(intent);
      return true;
    } else {
      return super.onOptionsItemSelected(item);
    }
  }

  public static HomeViewModel obtainViewModel(FragmentActivity activity) {
    return new ViewModelProvider(activity).get(HomeViewModel.class);
  }

  public void setupNavigationBar() {
    navbar.setOnItemSelectedListener(
        item -> {
          int id = item.getItemId();
          if (id == R.id.home_home_menu) {
            mViewModel.openHome(getSupportFragmentManager());
          } else if (id == R.id.home_connect_menu) {
            handleConnectNavigation();
          } else if (id == R.id.home_launch_menu) {
            handleLaunchNavigation();
          } else if (id == R.id.home_wallet_menu) {
            mViewModel.openWallet(getSupportFragmentManager());
          } else if (id == R.id.home_social_media_menu) {
            handleSocialMediaNavigation();
          }
          return true;
        });
  }

  private void handleSocialMediaNavigation() {
    if (mViewModel.getLAOs().getValue() == null) {
      ErrorUtils.logAndShow(
          getApplicationContext(), TAG, new NoLAOException(), R.string.error_no_lao);
      showHomeTab();
    } else {
      HomeViewModel.openSocialMedia(this);
    }
  }

  private void handleConnectNavigation() {
    if (checkWalletInitialization()) {
      openConnect();
    } else {
      showHomeTab();
    }
  }

  private void handleLaunchNavigation() {
    if (checkWalletInitialization()) {
      mViewModel.openLaunch(getSupportFragmentManager());
    } else {
      showHomeTab();
    }
  }

  public void openConnect() {
    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
      mViewModel.openQrCodeScanning(getSupportFragmentManager());
    } else {
      requestCameraPermission();
    }
  }

  public void requestCameraPermission() {
    // Setup result listener to open the connect tab once the permission is granted
    getSupportFragmentManager()
        .setFragmentResultListener(
            CameraPermissionFragment.REQUEST_KEY, this, (k, b) -> openConnect());

    mViewModel.openCameraPermission(getSupportFragmentManager(), getActivityResultRegistry());
  }

  /**
   * Checks the status the wallet initialization and log and display error if needed
   *
   * @return true if the wallet is already initialized, else return false and deals with error
   *     displaying and logging
   */
  private boolean checkWalletInitialization() {
    if (Boolean.FALSE.equals(mViewModel.isWalletSetUp())) {
      ErrorUtils.logAndShow(
          getApplicationContext(),
          TAG,
          new UninitializedWalletException(),
          R.string.uninitialized_wallet_exception);
      return false;
    }
    return true;
  }

  private void showHomeTab() {
    new Handler(Looper.getMainLooper())
        .postDelayed(
            () -> navbar.setSelectedItemId(R.id.home_home_menu),
            getResources().getInteger(R.integer.navigation_reversion_delay));
  }

  /** Factory method to create a fresh Intent that opens an HomeActivity */
  public static Intent newIntent(Context ctx) {
    return new Intent(ctx, HomeActivity.class);
  }

  /**
   * Set the current fragment in the container of the home activity
   *
   * @param manager the manager of the activity
   * @param id of the fragment
   * @param fragmentSupplier provides the fragment if it is missing
   */
  public static void setCurrentFragment(
      FragmentManager manager, @IdRes int id, Supplier<Fragment> fragmentSupplier) {
    ActivityUtils.setFragmentInContainer(
        manager, R.id.fragment_container_home, id, fragmentSupplier);
  }
}
