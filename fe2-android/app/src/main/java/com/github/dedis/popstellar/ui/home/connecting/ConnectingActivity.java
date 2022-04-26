package com.github.dedis.popstellar.ui.home.connecting;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.home.HomeActivity;
import com.github.dedis.popstellar.utility.ActivityUtils;

import java.util.function.Supplier;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ConnectingActivity extends AppCompatActivity {
  public static final String TAG = ConnectingActivity.class.getSimpleName();

  private ConnectingViewModel mViewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.connecting_activity);
    setConnectingFragment();

    String channelId = (String) (getIntent().getExtras()).get(getString(R.string.lao_id));

    mViewModel = obtainViewModel(this);

    // We make sure to be subscribed to events before launching the connection to lao process
    subscribeToEvents();
    mViewModel.handleOpenConnection(channelId);
  }

  private void subscribeToEvents() {
    mViewModel
        .getOpenLaoEvent()
        .observe(
            this,
            stringEvent -> {
              String laoId = stringEvent.getContentIfNotHandled();
              if (laoId != null) {
                openLaoDetailActivity(laoId);
              }
            });

    mViewModel
        .getOpenHomeEvent()
        .observe(
            this,
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                openHomeActivity();
              }
            });
  }

  public static ConnectingViewModel obtainViewModel(FragmentActivity activity) {
    return new ViewModelProvider(activity).get(ConnectingViewModel.class);
  }

  private void openLaoDetailActivity(String laoId) {
    Log.d(TAG, "Trying to open lao detail for lao with id " + laoId);

    Intent intent = new Intent(this, LaoDetailActivity.class);
    intent.putExtra(getString(R.string.lao_id), laoId);
    intent.putExtra(getString(R.string.fragment_to_open), getString(R.string.lao_detail_extra));

    startActivity(intent);
  }

  private void openHomeActivity() {
    Log.d(TAG, "Opening Home activity");

    Intent intent = new Intent(this, HomeActivity.class);
    startActivity(intent);
    finish();
  }

  private void setConnectingFragment() {
    setCurrentFragment(R.id.fragment_connecting, ConnectingFragment::newInstance);
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
    if (fragment == null) {
      fragment = fragmentSupplier.get();
    }

    // Set the new fragment in the container
    ActivityUtils.replaceFragmentInActivity(
        getSupportFragmentManager(), fragment, R.id.fragment_container_connecting);
  }
}
