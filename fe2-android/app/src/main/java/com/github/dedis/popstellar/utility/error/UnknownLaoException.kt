package com.github.dedis.popstellar.utility.error

import com.github.dedis.popstellar.R

/** An exception that indicates the lao with the specified id is not known to the app */
class UnknownLaoException : GenericException {
  constructor(laoId: String) : super("Lao with id $laoId is unknown")

  constructor() : super("Could not find a valid Lao")

  override val userMessage: Int
    get() = R.string.unknown_lao_exception

  override val userMessageArguments: Array<Any?>
    get() = arrayOfNulls(0)
}
