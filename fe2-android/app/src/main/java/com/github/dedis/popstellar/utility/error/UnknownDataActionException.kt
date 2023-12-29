package com.github.dedis.popstellar.utility.error

import com.github.dedis.popstellar.model.network.method.message.data.Data

class UnknownDataActionException(data: Data) :
    DataHandlingException(data, "The action " + data.action + " is unknown")
