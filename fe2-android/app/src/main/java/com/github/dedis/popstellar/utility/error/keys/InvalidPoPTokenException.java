package com.github.dedis.popstellar.utility.error.keys;

import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import java.util.Set;

public class InvalidPoPTokenException extends KeyException {

  private final Set<PublicKey> validTokens;

  public InvalidPoPTokenException(PoPToken token, Set<PublicKey> validTokens) {
    super("The token " + token.getPublicKey() + " is invalid");
    this.validTokens = validTokens;
  }

  public Set<PublicKey> getValidTokens() {
    return validTokens;
  }
}
