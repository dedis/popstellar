package com.github.dedis.popstellar.utility.error

import com.github.dedis.popstellar.R

class UnknownMeetingException(id: String) : UnknownEventException("Meeting", id) {
    override val userMessage: Int
        get() = R.string.unknown_meeting_exception
    override val userMessageArguments: Array<Any?>
        get() = arrayOfNulls(0)
}