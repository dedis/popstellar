package com.github.dedis.popstellar.utility.error.keys;

import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import java.util.Set;

public class InvalidPoPTokenException extends KeyException {
  private Set<PublicKey> validTokens;

  public InvalidPoPTokenException(PoPToken token, Set<PublicKey> validTokens) {
    super("The token " + token.getPublicKey() + " is invalid is it");
    this.validTokens = validTokens;
  }
}
