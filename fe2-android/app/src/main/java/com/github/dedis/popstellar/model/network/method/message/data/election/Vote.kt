package com.github.dedis.popstellar.model.network.method.message.data.election

/** This interface represent a basic vote type */
interface Vote {
  /** @return the id of the question this vote is referring to */
  val questionId: String

  /** @return the id of the vote. Its computation depends on its type */
  val id: String

  /** @return whether or not the vote is encrypted */
  val isEncrypted: Boolean
}
