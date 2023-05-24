package com.github.dedis.popstellar.utility.error;

import com.github.dedis.popstellar.R;

public class UnknownReactionException extends GenericException {

  public UnknownReactionException() {
    super("Deleting a reaction which is unknown");
  }

  @Override
  public int getUserMessage() {
    return R.string.unknown_reaction_exception;
  }

  @Override
  public Object[] getUserMessageArguments() {
    return new Object[0];
  }
}
