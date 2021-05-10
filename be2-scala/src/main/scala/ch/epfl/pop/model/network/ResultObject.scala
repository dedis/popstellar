package ch.epfl.pop.model.network

import ch.epfl.pop.model.network.method.message.Message

class ResultObject(resultInt: Option[Int], resultMessages: Option[List[Message]]) {

  def this(result: Int) = this(Some(result), None)
  def this(result: List[Message]) = this(None, Some(result))

  def isIntResult: Boolean = resultInt.isDefined
}
