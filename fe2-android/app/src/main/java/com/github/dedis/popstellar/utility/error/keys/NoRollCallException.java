package com.github.dedis.popstellar.utility.error.keys;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.Lao;

/** Exception thrown when a rollcall is expected to be found in an LAO and none exist */
public class NoRollCallException extends KeyException {

  public NoRollCallException(Lao lao) {
    super("No RollCall exist in the LAO : " + lao.getId());
  }

  @Override
  public int getUserMessage() {
    return R.string.no_rollcall_exception;
  }

  @Override
  public Object[] getUserMessageArguments() {
    return new Object[0];
  }
}
