package com.github.dedis.popstellar.utility.error

import com.github.dedis.popstellar.model.network.method.message.data.Data

class UnhandledDataTypeException(data: Data, type: String?) :
  DataHandlingException(
    data,
    "The pair (${data.`object`}, ${data.action}) is not handled by the system because of $type"
  )
