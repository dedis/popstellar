package com.github.dedis.popstellar.utility.error.keys

import com.github.dedis.popstellar.R

class SeedValidationException(cause: Exception?) :
    KeyException("Unable to validate given seed", cause) {
    override val userMessage: Int
        get() = R.string.seed_validation_exception
    override val userMessageArguments: Array<Any?>
        get() = arrayOfNulls(0)
}