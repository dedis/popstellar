package com.github.dedis.popstellar.utility.error;

import com.github.dedis.popstellar.R;

public class UnknownLaoException extends GenericException {

  public UnknownLaoException(String laoId) {
    super("Lao with id " + laoId + " is unknown");
  }

  public UnknownLaoException() {
    super("Could not find a valid Lao");
  }

  @Override
  public int getUserMessage() {
    return R.string.unknown_lao_exception;
  }

  @Override
  public Object[] getUserMessageArguments() {
    return new Object[0];
  }
}
