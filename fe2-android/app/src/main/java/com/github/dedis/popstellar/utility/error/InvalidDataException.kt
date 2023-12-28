package com.github.dedis.popstellar.utility.error

import com.github.dedis.popstellar.model.network.method.message.data.Data

open class InvalidDataException(data: Data, dataType: String, dataValue: String) :
    DataHandlingException(data, "Invalid $dataType $dataValue")