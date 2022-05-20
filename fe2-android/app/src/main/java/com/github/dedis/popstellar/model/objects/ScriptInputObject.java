package com.github.dedis.popstellar.model.objects;

public class ScriptInputObject {
  private final String type;
  private final String pub_key_recipient;
  private final String sig;

  /**
   * @param type The script describing the unlock mechanism
   * @param pub_key_recipient The recipient’s public key
   * @param sig Signature on all txins and txouts using the recipient's private key
   */
  public ScriptInputObject(String type, String pub_key_recipient, String sig) {
    this.type = type;
    this.pub_key_recipient = pub_key_recipient;
    this.sig = sig;
  }

  public String getSig() {
    return sig;
  }

  public String getType() {
    return type;
  }

  public String getPubkey() {
    return pub_key_recipient;
  }
}
