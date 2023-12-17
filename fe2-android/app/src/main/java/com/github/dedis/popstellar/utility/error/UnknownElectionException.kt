package com.github.dedis.popstellar.utility.error

import com.github.dedis.popstellar.R

class UnknownElectionException(electionId: String) : UnknownEventException("Election", electionId) {
    override val userMessage: Int
        get() = R.string.unknown_election_exception
    override val userMessageArguments: Array<Any?>
        get() = arrayOfNulls(0)
}