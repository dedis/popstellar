package com.github.dedis.popstellar.utility.error

import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.objects.security.MessageID

class UnknownWitnessMessageException(id: MessageID) :
    GenericException("Witness message with id " + id.encoded + " is unknown") {
  override val userMessage: Int
    get() = R.string.unknown_witness_message_exception

  override val userMessageArguments: Array<Any?>
    get() = arrayOfNulls(0)
}
