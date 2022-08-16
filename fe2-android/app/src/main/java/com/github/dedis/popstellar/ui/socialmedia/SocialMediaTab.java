package com.github.dedis.popstellar.ui.socialmedia;

import androidx.annotation.IdRes;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.ui.navigation.Tab;

import java.util.Arrays;
import java.util.List;

public enum SocialMediaTab implements Tab {
  HOME(R.id.social_media_home_menu),
  SEARCH(R.id.social_media_search_menu),
  FOLLOWING(R.id.social_media_following_menu),
  PROFILE(R.id.social_media_profile_menu);

  private static final List<SocialMediaTab> ALL = Arrays.asList(values());

  /**
   * Find a tab based on its menu id, throws an exception when no tab match the id
   *
   * @param menuId of the menu
   * @return the tab corresponding to the given menu id
   */
  public static SocialMediaTab findByMenu(@IdRes int menuId) {
    for (SocialMediaTab tab : ALL) {
      if (tab.menuId == menuId) {
        return tab;
      }
    }

    throw new IllegalArgumentException("Unknown id : " + menuId);
  }

  @IdRes private final int menuId;

  SocialMediaTab(@IdRes int menuId) {
    this.menuId = menuId;
  }

  @IdRes
  @Override
  public int getMenuId() {
    return menuId;
  }

  @Override
  public String getName() {
    return toString();
  }
}
