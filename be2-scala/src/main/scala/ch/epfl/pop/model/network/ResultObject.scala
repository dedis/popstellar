package ch.epfl.pop.model.network

import ch.epfl.pop.model.network.method.message.Message

class ResultObject(val resultInt: Option[Int], val resultMessages: Option[List[Message]]) {

  def this(result: Int) = this(Some(result), None)

  def this(result: List[Message]) = this(None, Some(result))

  def isIntResult: Boolean = resultInt.isDefined

  override def equals(o: Any): Boolean = {
      o match {
          case that: ResultObject =>
            this.resultInt == that.resultInt && that.resultMessages == this.resultMessages
          case _=> false
      }
    }
}
