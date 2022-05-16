package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

// The script describing the TxOut unlock mechanism
public class Script_output {
  @SerializedName("type")
  private final String type; // Type of script

  @SerializedName("pubkey_hash")
  private final String pub_key_hash; // Hash of the recipient’s public key

  /**
   * @param type Type of script
   * @param pub_key_hash Hash of the recipient’s public key
   */
  public Script_output(String type, String pub_key_hash) {
    this.type = type;
    this.pub_key_hash = pub_key_hash;
  }

  public String get_type() {
    return type;
  }

  public String get_pubkey_hash() {
    return pub_key_hash;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Script_output that = (Script_output) o;
    return Objects.equals(type, that.type) && Objects.equals(pub_key_hash, that.pub_key_hash);
  }

  @Override
  public int hashCode() {
    return Objects.hash(get_type(), get_pubkey_hash());
  }

  @Override
  public String toString() {
    return "script{" + "type='" + type + '\'' + ", pubkey_hash='" + pub_key_hash + '\'' + '}';
  }
}
