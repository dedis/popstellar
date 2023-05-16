package com.github.dedis.popstellar.utility.error;

import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

/** This exception is raised when a signature message from a non witness is received. */
public class InvalidWitnessException extends DataHandlingException {

  /**
   * Create a new invalid witness exception
   *
   * @param data that generated the exception
   * @param witnessPublicKey the invalid witness public key
   */
  public InvalidWitnessException(Data data, PublicKey witnessPublicKey) {
    super(data, "No witness with public key " + witnessPublicKey + " exists in the lao");
  }
}
