package com.github.dedis.popstellar.utility.error

import com.github.dedis.popstellar.R

/** Exception to be used when not a single Lao can be found  */
class NoLAOException : GenericException() {
    override val userMessage: Int
        get() = R.string.error_no_lao
    override val userMessageArguments: Array<Any?>
        get() = arrayOfNulls(0)
}