package com.github.dedis.popstellar.model.objects;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.objects.security.MessageID;

import java.util.Objects;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PendingUpdate that = (PendingUpdate) o;
    return modificationTime == that.modificationTime &&
        Objects.equals(messageId, that.messageId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(modificationTime, messageId);
  }
}
