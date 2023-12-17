package com.github.dedis.popstellar.utility.error

import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.network.answer.Error

/** This exception is thrown when an [Error] response is received from a server  */
class JsonRPCErrorException(error: Error) :
    GenericException("Error " + error.error.code + " - " + error.error.description) {
    private val errorCode: Int
    private val errorDesc: String

    init {
        errorCode = error.error.code
        errorDesc = error.error.description
    }

    override val userMessage: Int
        get() = R.string.json_rpc_exception
    override val userMessageArguments: Array<Any?>
        get() = arrayOf(errorCode, errorDesc)
}