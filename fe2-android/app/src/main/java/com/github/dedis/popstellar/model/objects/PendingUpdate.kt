package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.security.MessageID
import java.util.Objects

@Immutable
class PendingUpdate(val modificationTime: Long, val messageId: MessageID) :
    Comparable<PendingUpdate> {

  override operator fun compareTo(other: PendingUpdate): Int {
    return modificationTime.compareTo(other.modificationTime)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as PendingUpdate
    return modificationTime == that.modificationTime && messageId == that.messageId
  }

  override fun hashCode(): Int {
    return Objects.hash(modificationTime, messageId)
  }
}
