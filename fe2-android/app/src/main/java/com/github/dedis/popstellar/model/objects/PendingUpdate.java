package com.github.dedis.popstellar.model.objects;

import java.io.Serializable;

public class PendingUpdate implements Comparable<PendingUpdate>, Serializable {

  private final long modificationTime;
  private final String messageId;

  public PendingUpdate(long modificationTime, String messageId) {
    this.modificationTime = modificationTime;
    this.messageId = messageId;
  }

  public long getModificationTime() {
    return modificationTime;
  }

  public String getMessageId() {
    return messageId;
  }

  @Override
  public int compareTo(PendingUpdate o) {
    return Long.compare(modificationTime, o.modificationTime);
  }
}
