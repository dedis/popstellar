package com.github.dedis.popstellar.model.network.method.message.data

/** An abstract high level message */
abstract class Data {
  /** Returns the object the message is referring to. */
  abstract val `object`: String

  /** Returns the action the message is handling. */
  abstract val action: String
}
