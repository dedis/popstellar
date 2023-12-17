package com.github.dedis.popstellar.utility.error

import androidx.annotation.StringRes

abstract class GenericException : Exception {
    protected constructor(message: String?) : super(message)
    protected constructor(message: String?, cause: Throwable?) : super(message, cause)
    protected constructor()

    @get:StringRes
    abstract val userMessage: Int
    abstract val userMessageArguments: Array<Any?>
}