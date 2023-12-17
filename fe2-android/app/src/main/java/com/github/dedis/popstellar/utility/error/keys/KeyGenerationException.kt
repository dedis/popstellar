package com.github.dedis.popstellar.utility.error.keys

import com.github.dedis.popstellar.R
import java.security.GeneralSecurityException

class KeyGenerationException(e: GeneralSecurityException?) :
    KeyException("Could not generate key", e) {
    override fun getUserMessage(): Int {
        return R.string.key_generation_exception
    }

    override fun getUserMessageArguments(): Array<Any> {
        return arrayOf()
    }
}