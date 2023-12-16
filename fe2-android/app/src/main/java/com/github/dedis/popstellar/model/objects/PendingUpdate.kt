package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.security.MessageID
import java.util.Objects

@Immutable
class PendingUpdate(@JvmField val modificationTime: Long, @JvmField val messageId: MessageID) : Comparable<PendingUpdate> {

    override fun compareTo(o: PendingUpdate): Int {
        return modificationTime.compareTo(o.modificationTime)
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