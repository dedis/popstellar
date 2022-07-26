package com.github.dedis.popstellar.ui.socialmedia;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.*;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import dagger.hilt.android.AndroidEntryPoint;

/** Activity for the social media */
@AndroidEntryPoint
public class SocialMediaActivity extends AppCompatActivity {
  private SocialMediaViewModel mViewModel;

  public static final String LAO_ID = "LAO_ID";
  public static final String LAO_NAME = "LAO_NAME";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.social_media_activity);
    mViewModel = obtainViewModel(this);

    // When we launch the social media from a lao, it directly sets its id and name
    String laoId = getIntent().getExtras().getString(LAO_ID);
    String laoName = getIntent().getExtras().getString(LAO_NAME);

    if (laoId != null) mViewModel.setLaoId(laoId);
    if (laoName != null) mViewModel.setLaoName(laoName);

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

  public static SocialMediaViewModel obtainViewModel(FragmentActivity activity) {
    return new ViewModelProvider(activity).get(SocialMediaViewModel.class);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.social_media_top_menu, menu);

    // Get the submenu and clear its unique item. The item was needed to create the submenu
    SubMenu laosList = menu.findItem(R.id.laos_list).getSubMenu();

    // Adding all currently opened lao name to the submenu
    mViewModel
        .getLAOs()
        .observe(
            this,
            list -> {
              if (list != null) {
                laosList.clear();
                for (int i = 0; i < list.size(); ++i) {
                  // Creating a unique id using the index of the lao within the list
                  laosList.add(Menu.NONE, i, Menu.CATEGORY_CONTAINER, list.get(i).getName());
                }
              }
            });

    return true;
  }

  @SuppressLint("NonConstantResourceId")
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Retrieve the index of the lao within the list
    int i = item.getItemId();
    List<Lao> laos = mViewModel.getLAOs().getValue();
    if (laos != null && i >= 0 && i < laos.size()) {
      Lao lao = laos.get(i);
      mViewModel.setLaoId(lao.getId());
      mViewModel.setLaoName(lao.getName());
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  public void setupNavigationBar() {
    BottomNavigationView bottomNavigationView = findViewById(R.id.social_media_nav_bar);
    bottomNavigationView.setOnItemSelectedListener(
        item -> {
          int itemId = item.getItemId();
          if (itemId == R.id.social_media_home_menu) {
            mViewModel.openHome();
          } else if (itemId == R.id.social_media_search_menu) {
            mViewModel.openSearch();
          } else if (itemId == R.id.social_media_following_menu) {
            mViewModel.openFollowing();
          } else if (itemId == R.id.social_media_profile_menu) {
            mViewModel.openProfile();
          }
          return true;
        });
  }

  public void setupSocialMediaHomeFragment() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_social_media_home,
        SocialMediaHomeFragment::newInstance);
  }

  public void setupSocialMediaSearchFragment() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_social_media_search,
        SocialMediaSearchFragment::newInstance);
  }

  public void setupSocialMediaFollowingFragment() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_social_media_following,
        SocialMediaFollowingFragment::newInstance);
  }

  public void setupSocialMediaProfileFragment() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_social_media_profile,
        SocialMediaProfileFragment::newInstance);
  }

  public static Intent newIntent(Context ctx, String laoId, String laoName) {
    Intent intent = new Intent(ctx, SocialMediaActivity.class);
    intent.putExtra(LAO_ID, laoId);
    intent.putExtra(LAO_NAME, laoName);
    return intent;
  }

  public static Intent newIntent(Context ctx) {
    return new Intent(ctx, SocialMediaActivity.class);
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
        manager, R.id.fragment_container_social_media, id, fragmentSupplier);
  }
}
