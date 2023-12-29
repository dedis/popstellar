package com.github.dedis.popstellar.utility.error

import com.github.dedis.popstellar.R

class UnknownRollCallException(id: String) : UnknownEventException("Roll call", id) {
  override val userMessage: Int
    get() = R.string.unknown_roll_call_exception

  override val userMessageArguments: Array<Any?>
    get() = arrayOfNulls(0)
}
