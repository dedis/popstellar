package com.github.dedis.popstellar.model.objects;

public class ScriptInputObject {
  private final String type;
  private final String pubKeyRecipient;
  private final String sig;

  /**
   * @param type The script describing the unlock mechanism
   * @param pubKeyRecipient The recipient’s public key
   * @param sig Signature on all txins and txouts using the recipient's private key
   */
  public ScriptInputObject(String type, String pubKeyRecipient, String sig) {
    this.type = type;
    this.pubKeyRecipient = pubKeyRecipient;
    this.sig = sig;
  }

  public String getSig() {
    return sig;
  }

  public String getType() {
    return type;
  }

  public String getPubkey() {
    return pubKeyRecipient;
  }
}
