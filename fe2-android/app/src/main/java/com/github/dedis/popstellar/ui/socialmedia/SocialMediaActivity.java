package com.github.dedis.popstellar.ui.socialmedia;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.*;

import androidx.annotation.IdRes;
import androidx.fragment.app.*;
import androidx.lifecycle.ViewModelProvider;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.ui.navigation.NavigationActivity;
import com.github.dedis.popstellar.utility.ActivityUtils;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import dagger.hilt.android.AndroidEntryPoint;

/** Activity for the social media */
@AndroidEntryPoint
public class SocialMediaActivity extends NavigationActivity<SocialMediaTab> {

  private SocialMediaViewModel mViewModel;

  public static final String TAG = SocialMediaActivity.class.getSimpleName();
  public static final String LAO_ID = "LAO_ID";
  public static final String LAO_NAME = "LAO_NAME";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.social_media_activity);
    navigationViewModel = mViewModel = obtainViewModel(this);

    // When we launch the social media from a lao, it directly sets its id and name
    if (getIntent().getExtras() != null) {
      String laoId = getIntent().getExtras().getString(LAO_ID);
      String laoName = getIntent().getExtras().getString(LAO_NAME);

      if (laoId != null) mViewModel.setLaoId(laoId);
      if (laoName != null) mViewModel.setLaoName(laoName);
    }

    setupNavigationBar(findViewById(R.id.social_media_nav_bar));

    subscribeToLaoName();
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

  @Override
  protected SocialMediaTab findTabByMenu(int menuId) {
    return SocialMediaTab.findByMenu(menuId);
  }

  @Override
  protected boolean openTab(SocialMediaTab tab) {
    switch (tab) {
      case HOME:
        openHomeTab();
        break;
      case SEARCH:
        openSearchTab();
        break;
      case FOLLOWING:
        openFollowingTab();
        break;
      case PROFILE:
        openProfileTab();
        break;
      default:
        Log.w(TAG, "Unhandled tab type : " + tab);
    }
    return true;
  }

  @Override
  protected SocialMediaTab getDefaultTab() {
    return SocialMediaTab.HOME;
  }

  private void openHomeTab() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_social_media_home,
        SocialMediaHomeFragment::newInstance);
  }

  private void openSearchTab() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_social_media_search,
        SocialMediaSearchFragment::newInstance);
  }

  private void openFollowingTab() {
    setCurrentFragment(
        getSupportFragmentManager(),
        R.id.fragment_social_media_following,
        SocialMediaFollowingFragment::newInstance);
  }

  private void openProfileTab() {
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
