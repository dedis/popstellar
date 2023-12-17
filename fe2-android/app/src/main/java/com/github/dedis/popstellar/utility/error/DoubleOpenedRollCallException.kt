package com.github.dedis.popstellar.utility.error

import com.github.dedis.popstellar.R

/**
 * An exception that indicates the roll call cannot be opened as there's already another one in
 * progress
 */
class DoubleOpenedRollCallException(id: String) :
    GenericException("Impossible to open roll call id $id as another roll call is still open") {
    override val userMessage: Int
        get() = R.string.already_open_roll_call_exception
    override val userMessageArguments: Array<Any?>
        get() = arrayOfNulls(0)
}