package com.github.dedis.popstellar.utility.error;

import androidx.annotation.Nullable;

import com.github.dedis.popstellar.R;

public class UnknownLaoException extends GenericException {

  private final String laoId;

  public UnknownLaoException(String laoId) {
    this.laoId = laoId;
  }

  @Nullable
  @Override
  public String getMessage() {
    return "Lao with id " + laoId + " is unknown";
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
