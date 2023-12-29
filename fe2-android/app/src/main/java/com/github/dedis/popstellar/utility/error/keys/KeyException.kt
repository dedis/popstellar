package com.github.dedis.popstellar.utility.error.keys

import com.github.dedis.popstellar.utility.error.GenericException

/** This class regroup all the exceptions that can be generated when retrieving a key */
abstract class KeyException : GenericException {
  protected constructor(msg: String?, cause: Exception?) : super(msg, cause)

  protected constructor(msg: String?) : super(msg)
}
