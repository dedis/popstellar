package com.github.dedis.popstellar.utility.error;

import com.github.dedis.popstellar.R;

/**
 * An exception that indicates the roll call cannot be opened as there's already another one in
 * progress
 */
public class DoubleOpenedRollCallException extends GenericException {

  public DoubleOpenedRollCallException(String id) {
    super("Impossible to open roll call " + id + " as another roll call is still open");
  }

  @Override
  public int getUserMessage() {
    return R.string.already_open_roll_call_exception;
  }

  @Override
  public Object[] getUserMessageArguments() {
    return new Object[0];
  }
}
