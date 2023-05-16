package com.github.dedis.popstellar.utility.error;

import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.objects.security.MessageID;

/**
 * This exception is raised when a signature message for a non existent witness message is received.
 */
public class InvalidWitnessMessageException extends DataHandlingException {

  /**
   * Create a new invalid witness message exception
   *
   * @param data that generated the exception
   * @param messageID the invalid message ID
   */
  public InvalidWitnessMessageException(Data data, MessageID messageID) {
    super(data, "No witness message with id " + messageID + " exists in the lao");
  }
}
