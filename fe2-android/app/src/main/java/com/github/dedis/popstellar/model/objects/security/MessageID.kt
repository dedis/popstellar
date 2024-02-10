package com.github.dedis.popstellar.model.objects.security

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.utility.security.HashSHA256.hash

/** Represents the id of a message */
@Immutable
class MessageID : Base64URLData {
  constructor(data: String) : super(data)

  /**
   * Create the message id based on the data it transport and the sender's signature
   *
   * @param dataBuf json representation of the transferred data
   * @param signature the sender generated for the message
   */
  constructor(
      dataBuf: Base64URLData,
      signature: Signature
  ) : super(hash(dataBuf.encoded, signature.encoded))
}
