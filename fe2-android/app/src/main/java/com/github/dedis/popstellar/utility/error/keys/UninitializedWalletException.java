package com.github.dedis.popstellar.utility.error.keys;

import com.github.dedis.popstellar.R;

public class UninitializedWalletException extends KeyException {

  public UninitializedWalletException() {
    super("The wallet is not initialized");
  }

  @Override
  public int getUserMessage() {
    return R.string.uninitzilized_wallet_exception;
  }

  @Override
  public Object[] getUserMessageArguments() {
    return new Object[0];
  }
}
