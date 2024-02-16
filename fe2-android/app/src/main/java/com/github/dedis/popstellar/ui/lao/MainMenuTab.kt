package com.github.dedis.popstellar.ui.lao

import androidx.annotation.IdRes
import com.github.dedis.popstellar.R

enum class MainMenuTab(@get:IdRes val menuId: Int) {
  INVITE(R.id.main_menu_invite),
  EVENTS(R.id.main_menu_event_list),
  SOCIAL_MEDIA(R.id.main_menu_social_media),
  DIGITAL_CASH(R.id.main_menu_digital_cash),
  POPCHA(R.id.main_menu_popcha),
  WITNESSING(R.id.main_menu_witnessing),
  TOKENS(R.id.main_menu_tokens),
  DISCONNECT(R.id.main_menu_disconnect);

  companion object {
    private val ALL = values().asList()

    @JvmStatic
    fun findByMenu(@IdRes menuId: Int): MainMenuTab {
      return ALL.stream()
          .filter { tab: MainMenuTab -> tab.menuId == menuId }
          .findFirst()
          .orElseThrow { IllegalArgumentException("Unknown id : $menuId") }
    }
  }
}
