package com.github.dedis.popstellar.utility.error;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.security.MessageID;

public class UnknownWitnessMessageException extends GenericException {

  public UnknownWitnessMessageException(MessageID id) {
    super("Witness message with id " + id.getEncoded() + " is unknown");
  }

  @Override
  public int getUserMessage() {
    return R.string.unknown_witness_message_exception;
  }

  @Override
  public Object[] getUserMessageArguments() {
    return new Object[0];
  }
}
