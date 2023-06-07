package com.github.dedis.popstellar.utility.error;

import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

/** This exception is raised when an invalid witness signature message is received. */
public class InvalidWitnessingException extends DataHandlingException {

  /**
   * Create a new invalid witnessing exception for a witness signature received from a non witness
   *
   * @param data that generated the exception
   * @param witnessPublicKey the invalid witness public key
   */
  public InvalidWitnessingException(Data data, PublicKey witnessPublicKey) {
    super(data, "No witness with public key " + witnessPublicKey + " exists in the lao");
  }

  /**
   * Create a new invalid witness exception for a witness signature received for a non existent
   * witness message
   *
   * @param data that generated the exception
   * @param messageID the invalid message ID
   */
  public InvalidWitnessingException(Data data, MessageID messageID) {
    super(data, "No witness message with id " + messageID + " exists in the lao");
  }
}
