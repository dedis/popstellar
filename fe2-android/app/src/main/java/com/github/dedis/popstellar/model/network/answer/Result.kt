package com.github.dedis.popstellar.model.network.answer

/** A succeed query's answer  */
open class Result
/**
 * Constructor of a Result
 *
 * @param id of the answer
 */
(id: Int) : Answer(id) {
    override fun toString(): String {
        return "Result{id=$id}"
    }
}