package com.github.dedis.popstellar.utility.handler.data

import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.utility.error.DataHandlingException
import com.github.dedis.popstellar.utility.error.UnknownElectionException
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.error.UnknownRollCallException
import com.github.dedis.popstellar.utility.error.UnknownWitnessMessageException
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException

/**
 * Interface of functions used to handle data message. The generic type T of data need to be a
 * subclass of Data.
 */
fun interface DataHandler<T : Data?> {
    /**
     * @param context the HandlerContext of the message
     * @param data the Data to be handle
     * @throws DataHandlingException if an error occurs
     */
    @Throws(
        DataHandlingException::class,
        UnknownLaoException::class,
        UnknownRollCallException::class,
        UnknownElectionException::class,
        NoRollCallException::class,
        UnknownWitnessMessageException::class
    )
    fun accept(context: HandlerContext, data: T)
}