package com.github.dedis.popstellar.utility.error.keys

import com.github.dedis.popstellar.R

class UninitializedWalletException : KeyException("The wallet is not initialized") {
    override fun getUserMessage(): Int {
        return R.string.uninitialized_wallet_exception
    }

    override fun getUserMessageArguments(): Array<Any> {
        return arrayOf()
    }
}