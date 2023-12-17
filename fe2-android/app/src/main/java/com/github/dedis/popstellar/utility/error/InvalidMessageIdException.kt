package com.github.dedis.popstellar.utility.error

import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.objects.security.MessageID

class InvalidMessageIdException : InvalidDataException {
    constructor(data: Data, id: String) : super(data, "message id", id)
    constructor(data: Data, id: MessageID) : super(data, "message id", id.encoded)
}