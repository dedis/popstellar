package com.github.dedis.popstellar.utility.error

import com.github.dedis.popstellar.model.network.method.message.data.Data

class UnhandledDataTypeException(data: Data, type: String?) :
    DataHandlingException(
        data,
        String.format(
            "The pair (%s, %s) is not handled by the system because of %s",
            data.getObject(),
            data.action,
            type))
