package com.github.dedis.popstellar.utility.error.keys

import com.github.dedis.popstellar.R

class UninitializedWalletException : KeyException("The wallet is not initialized") {
    override val userMessage: Int
        get() = R.string.uninitialized_wallet_exception
    override val userMessageArguments: Array<Any?>
        get() = arrayOfNulls(0)
}