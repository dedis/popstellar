package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

// The script describing the unlock mechanism
public class ScriptTxIn {
  @SerializedName("Type")
  private final String type; // The script describing the unlock mechanism

  @SerializedName("Pubkey")
  private final String pub_key_recipient; // The recipient’s public key

  @SerializedName("Sig")
  private final String sig; // Signature on all txins and txouts using the recipient's private key
  // Transaction //with all txin txout

  /**
   * @param type The script describing the unlock mechanism
   * @param pub_key_recipient The recipient’s public key
   * @param sig Signature on all txins and txouts using the recipient's private key
   */
  public ScriptTxIn(String type, String pub_key_recipient, String sig) {
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

  public String getPub_key_recipient() {
    return pub_key_recipient;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ScriptTxIn that = (ScriptTxIn) o;
    return Objects.equals(type, that.type)
        && Objects.equals(pub_key_recipient, that.pub_key_recipient)
        && Objects.equals(sig, that.sig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), getPub_key_recipient(), getSig());
  }

  @Override
  public String toString() {
    return "ScriptTxIn{"
        + "type='"
        + type
        + '\''
        + ", pub_key_recipient='"
        + pub_key_recipient
        + '\''
        + ", sig='"
        + sig
        + '\''
        + '}';
  }
}
