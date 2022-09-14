package com.github.dedis.popstellar.utility.error;

import com.github.dedis.popstellar.R;

/** Exception to be used when not a single Lao can be found */
public class NoLAOException extends GenericException {

  @Override
  public int getUserMessage() {
    return R.string.error_no_lao;
  }

  @Override
  public Object[] getUserMessageArguments() {
    return new Object[0];
  }
}
