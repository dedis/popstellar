package com.github.dedis.popstellar.utility.error.keys

import com.github.dedis.popstellar.R

class SeedValidationException(cause: Exception?) :
    KeyException("Unable to validate given seed", cause) {
    override fun getUserMessage(): Int {
        return R.string.seed_validation_exception
    }

    override fun getUserMessageArguments(): Array<Any> {
        return arrayOf()
    }
}