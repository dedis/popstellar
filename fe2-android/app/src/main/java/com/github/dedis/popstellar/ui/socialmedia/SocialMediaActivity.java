package com.github.dedis.popstellar.ui.socialmedia;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.ViewModelFactory;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Objects;
import java.util.function.Supplier;

public class SocialMediaActivity extends AppCompatActivity {
  private final String TAG = SocialMediaActivity.class.getSimpleName();
  private SocialMediaViewModel mViewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.social_media_activity);
    mViewModel = obtainViewModel(this);
    // mViewModel.subscribeToChannel(
    //     (String) Objects.requireNonNull(getIntent().getExtras()).get("LAO_ID"));
    mViewModel.setLaoId((String) Objects.requireNonNull(getIntent().getExtras()).get("LAO_ID"));

    setupSocialMediaHomeFragment();
    setupNavigationBar();

    // Subscribe to "open home" event
    mViewModel
        .getOpenHomeEvent()
        .observe(
            this,
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setupSocialMediaHomeFragment();
              }
            });

    // Subscribe to "open search" event
    mViewModel
        .getOpenSearchEvent()
        .observe(
            this,
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setupSocialMediaSearchFragment();
              }
            });

    // Subscribe to "open following" event
    mViewModel
        .getOpenFollowingEvent()
        .observe(
            this,
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setupSocialMediaFollowingFragment();
              }
            });

    // Subscribe to "open profile" event
    mViewModel
        .getOpenProfileEvent()
        .observe(
            this,
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setupSocialMediaProfileFragment();
              }
            });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.subscription_general_channel, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.subscription_icon) {
      mViewModel.subscribeToGeneralChannel(mViewModel.getLaoId().getValue());
      return true;
    } else {
      return super.onOptionsItemSelected(item);
    }
  }

  public static SocialMediaViewModel obtainViewModel(FragmentActivity activity) {
    ViewModelFactory factory = ViewModelFactory.getInstance(activity.getApplication());
    SocialMediaViewModel viewModel =
        new ViewModelProvider(activity, factory).get(SocialMediaViewModel.class);
    return viewModel;
  }

  public void setupNavigationBar() {
    BottomNavigationView bottomNavigationView = findViewById(R.id.social_media_nav_bar);
    bottomNavigationView.setOnItemSelectedListener(listener);
  }

  public void setupSocialMediaHomeFragment() {
    setCurrentFragment(R.id.fragment_social_media_home, SocialMediaHomeFragment::newInstance);
  }

  public void setupSocialMediaSearchFragment() {
    setCurrentFragment(R.id.fragment_social_media_search, SocialMediaSearchFragment::newInstance);
  }

  public void setupSocialMediaFollowingFragment() {
    setCurrentFragment(
        R.id.fragment_social_media_following, SocialMediaFollowingFragment::newInstance);
  }

  public void setupSocialMediaProfileFragment() {
    setCurrentFragment(R.id.fragment_social_media_profile, SocialMediaProfileFragment::newInstance);
  }

  private BottomNavigationView.OnItemSelectedListener listener =
      new BottomNavigationView.OnItemSelectedListener() {
        @SuppressLint("NonConstantResourceId")
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
          switch (item.getItemId()) {
            case R.id.social_media_home_menu:
              mViewModel.openHome();
              break;
            case R.id.social_media_search_menu:
              mViewModel.openSearch();
              break;
            case R.id.social_media_following_menu:
              mViewModel.openFollowing();
              break;
            case R.id.social_media_profile_menu:
              mViewModel.openProfile();
              break;
            default:
          }
          return true;
        }
      };

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
        getSupportFragmentManager(), fragment, R.id.fragment_container_social_media);
  }
}
