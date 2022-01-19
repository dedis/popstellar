package com.github.dedis.popstellar.utility.error.keys;

import com.github.dedis.popstellar.model.objects.security.PoPToken;

public class InvalidPoPTokenException extends KeyException {

  public InvalidPoPTokenException(PoPToken token) {
    super("The token " + token.getPublicKey() + " is invalid");
  }
}
