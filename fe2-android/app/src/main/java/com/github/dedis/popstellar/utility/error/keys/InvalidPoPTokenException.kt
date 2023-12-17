package com.github.dedis.popstellar.utility.error.keys

import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.objects.security.PoPToken

class InvalidPoPTokenException(token: PoPToken) :
    KeyException("The token " + token.publicKey + " is invalid") {
    private val publicKey: String

    init {
        publicKey = token.publicKey.encoded
    }

    override fun getUserMessage(): Int {
        return R.string.invalid_pop_token_exception
    }

    override fun getUserMessageArguments(): Array<Any> {
        return arrayOf(publicKey)
    }
}