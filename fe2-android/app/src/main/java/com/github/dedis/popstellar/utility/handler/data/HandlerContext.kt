package com.github.dedis.popstellar.utility.handler.data

import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.repository.remote.MessageSender

class HandlerContext(
    val messageId: MessageID,
    val senderPk: PublicKey,
    val channel: Channel,
    val messageSender: MessageSender
)