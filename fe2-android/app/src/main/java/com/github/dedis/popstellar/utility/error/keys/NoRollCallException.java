package com.github.dedis.popstellar.utility.error.keys;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.view.LaoView;

/** Exception thrown when a rollcall is expected to be found in an LAO and none exist */
public class NoRollCallException extends KeyException {

  public NoRollCallException(String laoId) {
    super("No RollCall exist in the LAO : " + laoId);
  }

  public NoRollCallException(Lao lao) {
    this(lao.getId());
  }

  public NoRollCallException(LaoView laoView) {
    this(laoView.getId());
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
