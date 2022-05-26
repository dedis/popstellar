package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

// The script describing the unlock mechanism
public class ScriptInput {
  @SerializedName("type")
  private final String type; // The script describing the unlock mechanism

  @SerializedName("pubkey")
  private final String pubKeyRecipient; // The recipient’s public key

  @SerializedName("sig")
  private String sig; // Signature on all txins and txouts using the recipient's private key
  // Transaction //with all txin txout

  /**
   * @param type The script describing the unlock mechanism
   * @param pubKeyRecipient The recipient’s public key
   * @param sig Signature on all txins and txouts using the recipient's private key
   */
  public ScriptInput(String type, String pubKeyRecipient, String sig) {
    this.type = type;
    this.pubKeyRecipient = pubKeyRecipient;
    this.sig = sig;
  }

  public String getSig() {
    return sig;
  }

  public void setSig(String sig) {
    this.sig = sig;
  }

  public String getType() {
    return type;
  }

  public String getPubkey() {
    return pubKeyRecipient;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ScriptInput that = (ScriptInput) o;
    return Objects.equals(type, that.type)
        && Objects.equals(pubKeyRecipient, that.pubKeyRecipient)
        && Objects.equals(sig, that.sig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), getPubkey(), getSig());
  }

  @Override
  public String toString() {
    return "script{"
        + "type='"
        + type
        + '\''
        + ", pubkey='"
        + pubKeyRecipient
        + '\''
        + ", sig='"
        + sig
        + '\''
        + '}';
  }
}
