package com.github.dedis.popstellar.model.network.method

import com.github.dedis.popstellar.model.objects.Channel
import java.util.Objects

/**
 * A message that expect an answer later. Therefore, it has a unique id that will be linked with the
 * received answer
 */
abstract class Query
/**
 * Constructor for a Query
 *
 * @param channel name of the channel
 * @param id request ID of the query
 */ protected constructor(channel: Channel?,
                          /** Returns the request ID.  */
                          @field:Transient val requestId: Int) : Message(channel) {

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        if (!super.equals(other)) {
            return false
        }
        val query = other as Query
        return requestId == query.requestId
    }

    override fun hashCode(): Int {
        return Objects.hash(super.hashCode(), requestId)
    }

    override fun toString(): String {
        return (javaClass.simpleName
                + '{'
                + "id="
                + requestId
                + ", channel='"
                + channel
                + '\''
                + ", method='"
                + method
                + '\''
                + '}')
    }
}