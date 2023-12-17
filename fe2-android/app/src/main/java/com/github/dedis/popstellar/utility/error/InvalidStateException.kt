package com.github.dedis.popstellar.utility.error

import com.github.dedis.popstellar.model.network.method.message.data.Data

/** This exception is raised when a message is handled at the wrong state of an object  */
class InvalidStateException(data: Data, `object`: String, state: String, expected: String) :
    DataHandlingException(
        data,
        "The $`object` is in the wrong state. It is in $state expected $expected"
    )