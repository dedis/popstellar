package com.github.dedis.popstellar.model.network.method

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.Channel

/** Subscribe to a channel */
@Immutable
class Subscribe(channel: Channel?, id: Int) : Query(channel, id) {
  override val method: String
    get() = Method.SUBSCRIBE.method
}
