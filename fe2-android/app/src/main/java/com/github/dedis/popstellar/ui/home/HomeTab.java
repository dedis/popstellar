package com.github.dedis.popstellar.ui.home;

import androidx.annotation.IdRes;

import com.github.dedis.popstellar.R;

import java.util.Arrays;
import java.util.List;

public enum HomeTab {
  HOME(R.id.home_home_menu),
  CONNECT(R.id.home_connect_menu),
  LAUNCH(R.id.home_launch_menu),
  WALLET(R.id.home_wallet_menu),
  SOCIAL(R.id.home_social_media_menu);

  private static final List<HomeTab> ALL = Arrays.asList(values());

  public static HomeTab findByMenu(@IdRes int menuId) {
    for (HomeTab tab : ALL) {
      if (tab.menuId == menuId) return tab;
    }

    throw new IllegalStateException("Unknown id : " + menuId);
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
