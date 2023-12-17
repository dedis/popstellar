package com.github.dedis.popstellar.utility.error

import com.github.dedis.popstellar.R

class UnknownReactionException : GenericException("Deleting a reaction which is unknown") {
    override val userMessage: Int
        get() = R.string.unknown_reaction_exception
    override val userMessageArguments: Array<Any?>
        get() = arrayOfNulls(0)
}