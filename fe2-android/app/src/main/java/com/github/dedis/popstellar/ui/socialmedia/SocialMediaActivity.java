package com.github.dedis.popstellar.ui.socialmedia;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
  private BottomNavigationView navbar;

  public static final String TAG = SocialMediaActivity.class.getSimpleName();
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

    setupNavigationBar();

    subscribeToLaoName();
    subscribeToSelectedItemEvents();
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

  private void setupNavigationBar() {
    navbar = findViewById(R.id.social_media_nav_bar);
    navbar.setOnItemSelectedListener(
        item -> {
          int itemId = item.getItemId();
          if (itemId != mViewModel.getCurrentSelectedItem().getValue()) {
            // This prevents the update to be done multiple times. It is done here rather than
            // in viewModel because otherwise this would be executed twice
            mViewModel.setCurrentSelectedItem(itemId);
          } else {
            if (itemId == R.id.social_media_home_menu) {
              Log.d(TAG, "Opening home");
              openSocialMediaHomeFragment();
            } else if (itemId == R.id.social_media_search_menu) {
              Log.d(TAG, "Opening search");
              openSocialMediaSearchFragment();
            } else if (itemId == R.id.social_media_following_menu) {
              Log.d(TAG, "Opening following");
              openSocialMediaFollowingFragment();
            } else if (itemId == R.id.social_media_profile_menu) {
              Log.d(TAG, "Opening profile");
              openSocialMediaProfileFragment();
            }
          }
          return true;
        });
  }

  private void subscribeToLaoName() {
    // Subscribe to "lao name" string
    mViewModel
        .getLaoName()
        .observe(
            this,
            newLaoName -> {
              if (newLaoName == null) {
                return;
              }
              Objects.requireNonNull(getSupportActionBar())
                  .setTitle(String.format(getString(R.string.social_media_title), newLaoName));
            });
  }

  private void subscribeToSelectedItemEvents() {
    mViewModel
        .getCurrentSelectedItem()
        .observe(
            this,
            item -> {
              if (item != null) {
                navbar.setSelectedItemId(item);
              }
            });
  }

  private void openSocialMediaHomeFragment() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_social_media_home,
        SocialMediaHomeFragment::newInstance);
  }

  private void openSocialMediaSearchFragment() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_social_media_search,
        SocialMediaSearchFragment::newInstance);
  }

  private void openSocialMediaFollowingFragment() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_social_media_following,
        SocialMediaFollowingFragment::newInstance);
  }

  private void openSocialMediaProfileFragment() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_social_media_profile,
        SocialMediaProfileFragment::newInstance);
  }

  public static Intent newInstance(Context ctx, String laoId, String laoName) {
    Intent intent = new Intent(ctx, SocialMediaActivity.class);
    intent.putExtra(LAO_ID, laoId);
    intent.putExtra(LAO_NAME, laoName);
    return intent;
  }

  public static Intent newInstance(Context ctx) {
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
