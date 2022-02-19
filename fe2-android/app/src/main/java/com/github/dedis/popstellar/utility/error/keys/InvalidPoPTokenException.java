package com.github.dedis.popstellar.utility.error.keys;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.security.PoPToken;

public class InvalidPoPTokenException extends KeyException {

  private final String publicKey;

  public InvalidPoPTokenException(PoPToken token) {
    super("The token " + token.getPublicKey() + " is invalid");

    publicKey = token.getPublicKey().getEncoded();
  }

  @Override
  public int getUserMessage() {
    return R.string.invald_pop_token_exception;
  }

  @Override
  public Object[] getUserMessageArguments() {
    return new Object[] {publicKey};
  }
}
