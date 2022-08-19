package com.github.dedis.popstellar.model.objects.digitalcash;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.security.Signature;

@Immutable
public class ScriptInputObject {

  private final String type;
  private final PublicKey pubKeyRecipient;
  private final Signature sig;

  /**
   * @param type The script describing the unlock mechanism
   * @param pubKeyRecipient The recipient’s public key
   * @param sig Signature on all txins and txouts using the recipient's private key
   */
  public ScriptInputObject(String type, PublicKey pubKeyRecipient, Signature sig) {
    this.type = type;
    this.pubKeyRecipient = pubKeyRecipient;
    this.sig = sig;
  }

  public Signature getSig() {
    return sig;
  }

  public String getType() {
    return type;
  }

  public PublicKey getPubKey() {
    return pubKeyRecipient;
  }
}
