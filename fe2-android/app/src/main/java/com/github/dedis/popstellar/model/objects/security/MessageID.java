package com.github.dedis.popstellar.model.objects.security;

import com.github.dedis.popstellar.utility.security.Hash;

public class MessageID extends Base64URLData {

  public MessageID(String data) {
    super(data);
  }

  public MessageID(Base64URLData dataBuf, Signature signature) {
    super(Hash.hash(dataBuf.getEncoded(), signature.getEncoded()));
  }
}
