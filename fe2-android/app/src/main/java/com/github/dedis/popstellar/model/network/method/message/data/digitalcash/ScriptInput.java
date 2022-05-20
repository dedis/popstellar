package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

// The script describing the unlock mechanism
public class ScriptInput {
  @SerializedName("type")
  private final String type; // The script describing the unlock mechanism

  @SerializedName("pubkey")
  private final String pub_key_recipient; // The recipient’s public key

  @SerializedName("sig")
  private String sig; // Signature on all txins and txouts using the recipient's private key
  // Transaction //with all txin txout

  /**
   * @param type The script describing the unlock mechanism
   * @param pub_key_recipient The recipient’s public key
   * @param sig Signature on all txins and txouts using the recipient's private key
   */
  public ScriptInput(String type, String pub_key_recipient, String sig) {
    this.type = type;
    this.pub_key_recipient = pub_key_recipient;
    this.sig = sig;
  }

  public String get_sig() {
    return sig;
  }

  public void setSig(String sig) {
    this.sig = sig;
  }

  public String get_type() {
    return type;
  }

  public String get_pubkey() {
    return pub_key_recipient;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ScriptInput that = (ScriptInput) o;
    return Objects.equals(type, that.type)
        && Objects.equals(pub_key_recipient, that.pub_key_recipient)
        && Objects.equals(sig, that.sig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(get_type(), get_pubkey(), get_sig());
  }

  @Override
  public String toString() {
    return "script{"
        + "type='"
        + type
        + '\''
        + ", pubkey='"
        + pub_key_recipient
        + '\''
        + ", sig='"
        + sig
        + '\''
        + '}';
  }
}
