package com.github.dedis.popstellar.model.network.answer

import java.util.Objects

/** Error of a failed request  */
class ErrorCode
/**
 * Constructor of an ErrorCode
 *
 * @param code the code of the error, as an integer
 * @param description the description of the error
 */(
        /** Returns the code of the error.  */
        @JvmField val code: Int,
        /** Returns the description of the error.  */
        @JvmField val description: String) {

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as ErrorCode
        return this.code == that.code && description == that.description
    }

    override fun hashCode(): Int {
        return Objects.hash(this.code, description)
    }

    override fun toString(): String {
        return "ErrorCode{code=$code, description='$description'}"
    }
}