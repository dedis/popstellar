package com.github.dedis.popstellar

class SingleEvent<T>(content: T?) {
    private val mContent: T
    private var hasBeenHandled = false

    init {
        requireNotNull(content) { "null values not allowed in an Event" }
        mContent = content
    }

    val contentIfNotHandled: T?
        get() = if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            mContent
        }

    fun hasBeenHandled(): Boolean {
        return hasBeenHandled
    }
}