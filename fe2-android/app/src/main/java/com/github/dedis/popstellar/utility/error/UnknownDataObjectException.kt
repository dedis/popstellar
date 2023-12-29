package com.github.dedis.popstellar.utility.error

import com.github.dedis.popstellar.model.network.method.message.data.Data

class UnknownDataObjectException(data: Data) :
    DataHandlingException(data, "The object " + data.getObject() + " is unknown")
