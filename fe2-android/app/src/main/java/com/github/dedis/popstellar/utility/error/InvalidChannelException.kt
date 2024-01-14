package com.github.dedis.popstellar.utility.error

import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.objects.Channel

/** This exception is raised when a message is received on another channel than the one expected */
class InvalidChannelException
/**
 * Create a new invalid channel exception
 *
 * @param data that generated the exception
 * @param expectedKind of channel. Describes the type of expected channel ex: 'an lao channel'
 * @param got channel on which the message was received
 */
(data: Data, expectedKind: String, got: Channel) :
    DataHandlingException(
        data, "The message was received on channel $got but expected $expectedKind")
