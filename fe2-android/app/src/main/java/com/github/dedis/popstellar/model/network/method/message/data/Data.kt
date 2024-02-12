package com.github.dedis.popstellar.model.network.method.message.data

/** An abstract high level message */
interface Data {
  /** Returns the object the message is referring to. */
  val `object`: String

  /** Returns the action the message is handling. */
  val action: String
}
