package com.github.dedis.popstellar.model.objects;

public class ScriptOutputObject {

  // Type of script
  private final String type;
  // Hash of the recipient’s public key
  private final String pubKeyHash;

  /**
   * @param type Type of script
   * @param pubKeyHash Hash of the recipient’s public key
   */
  public ScriptOutputObject(String type, String pubKeyHash) {
    this.type = type;
    this.pubKeyHash = pubKeyHash;
  }

  public String getType() {
    return type;
  }

  public String getPubKeyHash() {
    return pubKeyHash;
  }

  // a function that given a public key verify it is the goof public key

}
