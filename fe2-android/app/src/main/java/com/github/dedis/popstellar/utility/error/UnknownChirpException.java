package com.github.dedis.popstellar.utility.error;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.security.MessageID;

public class UnknownChirpException extends GenericException {

  public UnknownChirpException(MessageID id) {
    super("Chirp with id " + id + " is unknown.");
  }

  @Override
  public int getUserMessage() {
    return R.string.unknown_chirp_exception;
  }

  @Override
  public Object[] getUserMessageArguments() {
    return new Object[0];
  }
}
