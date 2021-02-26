package com.github.dedis.student20_pop.model;

public class PendingUpdate implements Comparable<PendingUpdate> {

  private long modificationTime;
  private String messageId;

  public PendingUpdate(long modificationTime, String messageId) {
    this.modificationTime = modificationTime;
    this.messageId = messageId;
  }

  public long getModificationTime() {
    return modificationTime;
  }

  @Override
  public int compareTo(PendingUpdate o) {
    return Long.compare(modificationTime, o.modificationTime);
  }
}
