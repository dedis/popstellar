package com.github.dedis.popstellar.ui.lao;

import androidx.annotation.IdRes;

import com.github.dedis.popstellar.R;

import java.util.Arrays;
import java.util.List;

public enum MainMenuTab {
  INVITE(R.id.main_menu_invite),
  EVENTS(R.id.main_menu_event_list),
  SOCIAL_MEDIA(R.id.main_menu_social_media),
  DIGITAL_CASH(R.id.main_menu_digital_cash),
  WITNESSING(R.id.main_menu_witnessing),
  TOKENS(R.id.main_menu_tokens),
  DISCONNECT(R.id.main_menu_disconnect);

  private static final List<MainMenuTab> ALL = Arrays.asList(values());

  public static MainMenuTab findByMenu(@IdRes int menuId) {
    return ALL.stream()
        .filter(tab -> tab.menuId == menuId)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown id : " + menuId));
  }

  @IdRes private final int menuId;

  MainMenuTab(@IdRes int menuId) {
    this.menuId = menuId;
  }

  @IdRes
  public int getMenuId() {
    return menuId;
  }

  public String getName() {
    return toString();
  }
}
