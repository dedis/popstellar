package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import com.github.dedis.popstellar.model.Immutable;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

// The script describing the TxOut unlock mechanism
@Immutable
public final class ScriptOutput {

  @SerializedName("type")
  private final String type; // Type of script

  @SerializedName("pubkey_hash")
  private final String pubKeyHash; // Hash of the recipient’s public key

  /**
   * @param type Type of script
   * @param pubKeyHash Hash of the recipient’s public key
   */
  public ScriptOutput(String type, String pubKeyHash) {
    this.type = type;
    this.pubKeyHash = pubKeyHash;
  }

  public String getType() {
    return type;
  }

  public String getPubKeyHash() {
    return pubKeyHash;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ScriptOutput that = (ScriptOutput) o;
    return Objects.equals(type, that.type) && Objects.equals(pubKeyHash, that.pubKeyHash);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), getPubKeyHash());
  }

  @Override
  public String toString() {
    return "script{" + "type='" + type + '\'' + ", pubkey_hash='" + pubKeyHash + '\'' + '}';
  }
}
