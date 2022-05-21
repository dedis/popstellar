package com.github.dedis.popstellar.model.objects;

public class ScriptOutputObject {

  // Type of script
  private final String type;
  // Hash of the recipient’s public key
  private final String pub_key_hash;

  /**
   * @param type Type of script
   * @param pub_key_hash Hash of the recipient’s public key
   */
  public ScriptOutputObject(String type, String pub_key_hash) {
    this.type = type;
    this.pub_key_hash = pub_key_hash;
  }

  public String getType() {
    return type;
  }

  public String getPubkeyHash() {
    return pub_key_hash;
  }

  // a function that given a public key verify it is the goof public key

}
