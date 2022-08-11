package com.github.dedis.popstellar.ui.detail;

import androidx.annotation.IdRes;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.ui.navigation.Tab;

import java.util.Arrays;
import java.util.List;

/** Enum where each element represent a tab in LaoActivity */
public enum LaoTab implements Tab {
  EVENTS(R.id.lao_detail_event_list_menu),
  IDENTITY(R.id.lao_detail_identity_menu),
  WITNESSING(R.id.lao_detail_witnessing_menu),
  DIGITAL_CASH(R.id.lao_detail_digital_cash_menu),
  SOCIAL(R.id.lao_detail_social_media_menu);

  private static final List<LaoTab> ALL = Arrays.asList(values());

  /**
   * Find a tab based on its menu id, throws an exception when no the match the id
   *
   * @param menuId of the menu
   * @return the tab corresponding to the given menu id
   */
  public static LaoTab findByMenu(@IdRes int menuId) {
    for (LaoTab tab : ALL) {
      if (tab.menuId == menuId) {
        return tab;
      }
    }

    throw new IllegalArgumentException("Unknown id : " + menuId);
  }

  @IdRes private final int menuId;

  LaoTab(@IdRes int menuId) {
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
