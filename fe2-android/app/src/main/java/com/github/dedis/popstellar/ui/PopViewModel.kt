package com.github.dedis.popstellar.ui

import androidx.annotation.StringRes

interface PopViewModel {
  fun setPageTitle(@StringRes title: Int)

  val laoId: String?
}
