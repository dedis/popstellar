package ch.epfl.pop.model.network

import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.Channel

class ResultObject(val resultInt: Option[Int], val resultMessages: Option[List[Message]], val resultMap: Option[Map[Channel, Set[Message]]]) {

  def this(result: Int) = this(Some(result), None, None)

  def this(result: List[Message]) = this(None, Some(result), None)

  def this(mapResult: Map[Channel, Set[Message]]) = this(None, None, Some(mapResult))

  def isIntResult: Boolean = resultInt.isDefined

  override def equals(o: Any): Boolean = {
    o match {
      case that: ResultObject =>
        this.resultInt == that.resultInt && that.resultMessages == this.resultMessages && that.resultMap == this.resultMap
      case _ => false
    }
  }
}
