package com.github.dedis.popstellar.model.objects;

public class Script_input_object {
  private final String type;
  private final String pub_key_recipient;
  private final String sig;

  /**
   * @param type The script describing the unlock mechanism
   * @param pub_key_recipient The recipient’s public key
   * @param sig Signature on all txins and txouts using the recipient's private key
   */
  public Script_input_object(String type, String pub_key_recipient, String sig) {
    this.type = type;
    this.pub_key_recipient = pub_key_recipient;
    this.sig = sig;
  }

  public String get_sig() {
    return sig;
  }

  public String get_type() {
    return type;
  }

  public String get_pubkey() {
    return pub_key_recipient;
  }
}
