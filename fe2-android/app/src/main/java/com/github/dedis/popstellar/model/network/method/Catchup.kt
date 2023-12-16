package com.github.dedis.popstellar.model.network.method

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.Channel

/** Query to catchup on missed messages  */
@Immutable
class Catchup(channel: Channel?, id: Int) : Query(channel, id) {
    override val method: String
        get() = Method.CATCHUP.method
}