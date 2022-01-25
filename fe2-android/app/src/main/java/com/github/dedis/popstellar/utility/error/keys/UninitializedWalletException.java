package com.github.dedis.popstellar.utility.error.keys;

public class UninitializedWalletException extends KeyException {

  public UninitializedWalletException() {
    super("The wallet is not initialized");
  }
}
