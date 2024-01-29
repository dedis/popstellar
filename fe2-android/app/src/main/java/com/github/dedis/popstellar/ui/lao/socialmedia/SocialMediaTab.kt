package com.github.dedis.popstellar.ui.lao.socialmedia

import androidx.annotation.IdRes
import com.github.dedis.popstellar.R

enum class SocialMediaTab(@JvmField @get:IdRes val menuId: Int) {
  HOME(R.id.social_media_home_menu),
  SEARCH(R.id.social_media_search_menu),
  FOLLOWING(R.id.social_media_following_menu),
  PROFILE(R.id.social_media_profile_menu);

  companion object {
    private val ALL = values().asList()

    /**
     * Find a tab based on its menu id, throws an exception when no tab match the id
     *
     * @param menuId of the menu
     * @return the tab corresponding to the given menu id
     */
    @JvmStatic
    fun findByMenu(@IdRes menuId: Int): SocialMediaTab {
      return ALL.stream()
          .filter { tab: SocialMediaTab -> tab.menuId == menuId }
          .findFirst()
          .orElseThrow { IllegalArgumentException("Unknown id : $menuId") }
    }
  }
}
