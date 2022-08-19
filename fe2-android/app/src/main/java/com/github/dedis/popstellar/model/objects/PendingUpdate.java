package com.github.dedis.popstellar.model.objects;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.objects.security.MessageID;

@Immutable
public class PendingUpdate implements Comparable<PendingUpdate> {

  private final long modificationTime;
  private final MessageID messageId;

  public PendingUpdate(long modificationTime, MessageID messageId) {
    this.modificationTime = modificationTime;
    this.messageId = messageId;
  }

  public long getModificationTime() {
    return modificationTime;
  }

  public MessageID getMessageId() {
    return messageId;
  }

  @Override
  public int compareTo(PendingUpdate o) {
    return Long.compare(modificationTime, o.modificationTime);
  }
}
