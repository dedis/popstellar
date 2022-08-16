package com.github.dedis.popstellar.ui.digitalcash;

import androidx.annotation.IdRes;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.ui.navigation.Tab;

import java.util.Arrays;
import java.util.List;

public enum DigitalCashTab implements Tab {
  HOME(R.id.digital_cash_home_menu),
  HISTORY(R.id.digital_cash_history_menu),
  SEND(R.id.digital_cash_send_menu),
  RECEIVE(R.id.digital_cash_receive_menu),
  ISSUE(R.id.digital_cash_issue_menu);

  private static final List<DigitalCashTab> ALL = Arrays.asList(values());

  /**
   * Find a tab based on its menu id, throws an exception when no tab match the id
   *
   * @param menuId of the menu
   * @return the tab corresponding to the given menu id
   */
  public static DigitalCashTab findByMenu(@IdRes int menuId) {
    for (DigitalCashTab tab : ALL) {
      if (tab.menuId == menuId) {
        return tab;
      }
    }

    throw new IllegalArgumentException("Unknown id : " + menuId);
  }

  @IdRes private final int menuId;

  DigitalCashTab(@IdRes int menuId) {
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
