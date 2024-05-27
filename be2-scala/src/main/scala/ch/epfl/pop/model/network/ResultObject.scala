package ch.epfl.pop.model.network

import ch.epfl.pop.model.network.method.Rumor
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.{Channel, RumorStateAns}

sealed trait ResultType
final case class ResultInt(result: Int) extends ResultType
final case class ResultMessage(result: List[Message]) extends ResultType
final case class ResultMap(result: Map[Channel, Set[Message]]) extends ResultType
final case class ResultRumor(result: List[Rumor]) extends ResultType


class ResultObject(val result: Option[ResultType]) {

  // sugar syntax and legacy purposes
  def this(result : Int) = this(Some(ResultInt(result)))
  def this(result: Map[Channel, Set[Message]]) = this(Some(ResultMap(result)))
    
  def this(result : ResultType) = this(Some(result))

  def resultInt : Option[Int] = {
    result match
      case Some(resultInt: ResultInt) => Some(resultInt.result)
      case _ => None
  }

  def resultMessages: Option[List[Message]] = {
    result match
      case Some(resultMessage: ResultMessage) => Some(resultMessage.result)
      case _ => None
  }

  def resultMap: Option[Map[Channel, Set[Message]]] = {
    result match
      case Some(resultMap: ResultMap) => Some(resultMap.result)
      case _ => None
  }

  def resultRumor: Option[List[Rumor]] = {
    result match
      case Some(resultRumor: ResultRumor) => Some(resultRumor.result)
      case _ => None
  }

  def isIntResult: Boolean = result match
    case Some(_: ResultInt) => true
    case _ => false

  override def equals(obj: Any): Boolean = {
    obj match
      case that: ResultObject =>
        (this.result, that.result) match
          case (Some(a), Some(b)) => a == b
          case (None, None) => true
          case _ => false
  }
}

class ResultObject2(val resultInt: Option[Int], val resultMessages: Option[List[Message]], val resultMap: Option[Map[Channel, Set[Message]]], val resultRumor: Option[RumorStateAns]) {

  def this(result: Int) = this(Some(result), None, None, None)

  def this(result: List[Message]) = this(None, Some(result), None, None)

  def this(mapResult: Map[Channel, Set[Message]]) = this(None, None, Some(mapResult), None)

  def this(rumorResult: RumorStateAns) = this(None, None, None, Some(rumorResult))

  def isIntResult: Boolean = resultInt.isDefined

  override def equals(o: Any): Boolean = {
    o match {
      case that: ResultObject =>
        this.resultInt == that.resultInt && that.resultMessages == this.resultMessages && that.resultMap == this.resultMap
      case _ => false
    }
  }
}
