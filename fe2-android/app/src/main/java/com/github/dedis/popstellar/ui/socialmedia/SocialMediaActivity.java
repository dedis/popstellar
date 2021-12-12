package com.github.dedis.popstellar.ui.socialmedia;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SocialMediaActivity extends AppCompatActivity {
  private SocialMediaViewModel mViewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.social_media_activity);
    mViewModel = obtainViewModel(this);

    // When we launch the social media from the home activity, it has the list of all opened laos
    // but when it is launched in a lao, it only has its id
    if (getIntent().getExtras().get("OPENED_FROM").equals("HomeActivity")) {
      mViewModel.setLAOs(
          (List<Lao>)
              Objects.requireNonNull(getIntent().getBundleExtra("extra").getSerializable("LAOS")));
    } else if (getIntent().getExtras().get("OPENED_FROM").equals("LaoDetailActivity")) {
      Lao lao = (Lao) getIntent().getExtras().get("LAO");
      mViewModel.setLAOs(Collections.singletonList(lao));
      mViewModel.setLaoId(lao.getId());
      mViewModel.setLaoName(lao.getName());
    }

    setupSocialMediaHomeFragment();
    setupNavigationBar();

    // Subscribe to "lao name" string
    mViewModel
        .getLaoName()
        .observe(
            this,
            newLaoName -> {
              if (newLaoName != null) {
                Objects.requireNonNull(getSupportActionBar())
                    .setTitle(String.format("popstellar - %s", newLaoName));
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
    getMenuInflater().inflate(R.menu.social_media_top_menu, menu);

    // Get the submenu and clear its unique item. The item was needed to create the submenu
    SubMenu laosList = menu.findItem(R.id.laos_list).getSubMenu();
    laosList.clear();

    // Adding all currently opened lao name to the submenu
    for (int i = 0; i < Objects.requireNonNull(mViewModel.getLAOs().getValue()).size(); ++i) {
      // Creating a unique id using subscription_icon and laos_list such that it doesn't override
      // them in onOptionsItemSelected
      laosList.add(
          Menu.NONE,
          R.id.subscription_icon + R.id.laos_list + i,
          i,
          mViewModel.getLAOs().getValue().get(i).getName());
    }
    return true;
  }

  @SuppressLint("NonConstantResourceId")
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.subscription_icon) {
      mViewModel.subscribeToGeneralChannel(mViewModel.getLaoId().getValue());
      return true;
    } else {
      // Retrieve the index of the lao within the list
      int i = item.getItemId() - R.id.subscription_icon - R.id.laos_list;
      if (i >= 0) {
        mViewModel.setLaoId(Objects.requireNonNull(mViewModel.getLAOs().getValue()).get(i).getId());
        mViewModel.setLaoName(mViewModel.getLAOs().getValue().get(i).getName());
        return true;
      }
      return super.onOptionsItemSelected(item);
    }
  }

  public static SocialMediaViewModel obtainViewModel(FragmentActivity activity) {
    return new ViewModelProvider(activity).get(SocialMediaViewModel.class);
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
