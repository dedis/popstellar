package com.github.dedis.popstellar.model.network.answer

import java.util.Objects

/** A failed query's answer  */
class Error
/**
 * Constructor of an Error
 *
 * @param id of the answer
 * @param error of the answer, contains its code and description
 */(id: Int,
    /** Returns the error code.  */
    @JvmField val error: ErrorCode) : Answer(id) {

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        if (!super.equals(other)) {
            return false
        }
        val error = other as Error
        return this.error == error.error
    }

    override fun hashCode(): Int {
        return Objects.hash(super.hashCode(), error)
    }

    override fun toString(): String {
        return "Error{error=$error}"
    }
}