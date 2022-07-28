package com.github.dedis.popstellar.model.objects.security;

import com.github.dedis.popstellar.utility.security.Hash;

/** Represents the id of a message */
public class MessageID extends Base64URLData {

  public MessageID(String data) {
    super(data);
  }

  public MessageID(MessageID messageID) {
    super(messageID.data); // Deep copy of byte array is done in parent constructor
  }
  /**
   * Create the message id based on the data it transport and the sender's signature
   *
   * @param dataBuf json representation of the transferred data
   * @param signature the sender generated for the message
   */
  public MessageID(Base64URLData dataBuf, Signature signature) {
    super(Hash.hash(dataBuf.getEncoded(), signature.getEncoded()));
  }
}
