package com.github.dedis.popstellar.utility.error

import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PublicKey

/** This exception is raised when an invalid witness signature message is received. */
class InvalidWitnessingException : DataHandlingException {
  /**
   * Create a new invalid witnessing exception for a witness signature received from a non witness
   *
   * @param data that generated the exception
   * @param witnessPublicKey the invalid witness public key
   */
  constructor(
      data: Data,
      witnessPublicKey: PublicKey
  ) : super(data, "No witness with public key $witnessPublicKey exists in the lao")

  /**
   * Create a new invalid witness exception for a witness signature received for a non existent
   * witness message
   *
   * @param data that generated the exception
   * @param messageID the invalid message ID
   */
  constructor(
      data: Data,
      messageID: MessageID
  ) : super(data, "No witness message with id $messageID exists in the lao")
}
