package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

// The script describing the TxOut unlock mechanism
public class ScriptTxOut {
  @SerializedName("Type")
  private final String type; // Type of script

  @SerializedName("PubkeyHash")
  private final String pub_key_hash; // Hash of the recipient’s public key

  /**
   * @param type Type of script
   * @param pub_key_hash Hash of the recipient’s public key
   */
  public ScriptTxOut(String type, String pub_key_hash) {
    this.type = type;
    this.pub_key_hash = pub_key_hash;
  }

  public String getType() {
    return type;
  }

  public String getPub_key_hash() {
    return pub_key_hash;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ScriptTxOut that = (ScriptTxOut) o;
    return Objects.equals(type, that.type) && Objects.equals(pub_key_hash, that.pub_key_hash);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), getPub_key_hash());
  }

  @Override
  public String toString() {
    return "ScriptTxOut{" + "type='" + type + '\'' + ", pub_key_hash='" + pub_key_hash + '\'' + '}';
  }
}
