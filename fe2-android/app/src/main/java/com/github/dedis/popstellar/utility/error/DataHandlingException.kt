package com.github.dedis.popstellar.utility.error

import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.network.method.message.data.Data

open class DataHandlingException : GenericException {
    @Transient
    val data: Data

    constructor(data: Data, message: String?) : super(message) {
        this.data = data
    }

    constructor(data: Data, message: String?, cause: Throwable?) : super(message, cause) {
        this.data = data
    }

    override val message: String?
        get() = """
               Error while handling data : ${super.message}
               data=$data
               """.trimIndent()
    override val userMessage: Int
        get() = R.string.data_handling_exception
    override val userMessageArguments: Array<Any?>
        get() = arrayOf(data.javaClass.simpleName)
}