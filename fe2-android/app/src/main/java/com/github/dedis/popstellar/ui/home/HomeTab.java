package com.github.dedis.popstellar.ui.home;

import androidx.annotation.IdRes;

import com.github.dedis.popstellar.R;

import java.util.Arrays;
import java.util.List;

/** Enum where each element represent a tab in HomeActivity */
public enum HomeTab {
  HOME(R.id.home_home_menu),
  CONNECT(R.id.home_connect_menu),
  LAUNCH(R.id.home_launch_menu),
  WALLET(R.id.home_wallet_menu),
  SOCIAL(R.id.home_social_media_menu);

  private static final List<HomeTab> ALL = Arrays.asList(values());

  /**
   * Find a tab based on its menu id, throws an exception when no the match the id
   *
   * @param menuId of the menu
   * @return the tab corresponding to the given menu id
   */
  public static HomeTab findByMenu(@IdRes int menuId) {
    for (HomeTab tab : ALL) {
      if (tab.menuId == menuId) {
        return tab;
      }
    }

    throw new IllegalArgumentException("Unknown id : " + menuId);
  }

  @IdRes private final int menuId;

  HomeTab(@IdRes int menuId) {
    this.menuId = menuId;
  }

  @IdRes
  public int getMenuId() {
    return menuId;
  }
}
