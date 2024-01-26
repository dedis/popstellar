package com.github.dedis.popstellar.model

import androidx.annotation.StringRes
import com.github.dedis.popstellar.R

enum class Role(@StringRes val stringId: Int) {
  ORGANIZER(R.string.organizer),
  WITNESS(R.string.witness),
  ATTENDEE(R.string.attendee),
  MEMBER(R.string.member)
}
