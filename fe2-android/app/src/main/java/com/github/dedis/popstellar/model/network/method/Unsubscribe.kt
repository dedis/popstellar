package com.github.dedis.popstellar.model.network.method

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.Channel

/** Unsubscribe from a channel */
@Immutable
class Unsubscribe(channel: Channel?, id: Int) : Query(channel, id) {
  override val method: String
    get() = Method.UNSUBSCRIBE.method
}
