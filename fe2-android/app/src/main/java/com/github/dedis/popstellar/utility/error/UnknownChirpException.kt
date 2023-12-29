package com.github.dedis.popstellar.utility.error

import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.objects.security.MessageID

class UnknownChirpException(id: MessageID) : GenericException("Chirp with id $id is unknown.") {
  override val userMessage: Int
    get() = R.string.unknown_chirp_exception

  override val userMessageArguments: Array<Any?>
    get() = arrayOfNulls(0)
}
