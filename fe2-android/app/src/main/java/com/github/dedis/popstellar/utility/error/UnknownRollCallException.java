package com.github.dedis.popstellar.utility.error;

import com.github.dedis.popstellar.R;

public class UnknownRollCallException extends GenericException {

  public UnknownRollCallException(String id) {
    super("Roll call with id " + id + " is unknown.");
  }

  @Override
  public int getUserMessage() {
    return R.string.unknown_roll_call_exception;
  }

  @Override
  public Object[] getUserMessageArguments() {
    return new Object[0];
  }
}
