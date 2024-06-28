package ch.epfl.pop.model.network

import ch.epfl.pop.model.network.method.Rumor
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.Channel

sealed trait ResultType
final case class ResultInt(result: Int) extends ResultType
final case class ResultMessage(result: List[Message]) extends ResultType
final case class ResultMap(result: Map[Channel, Set[Message]]) extends ResultType
final case class ResultRumor(result: List[Rumor]) extends ResultType
final case class ResultEmptyList() extends ResultType

class ResultObject(val result: Option[ResultType]) {

  // sugar syntax and legacy purposes
  def this(result: Int) = this(Some(ResultInt(result)))
  def this(result: Map[Channel, Set[Message]]) = this(Some(ResultMap(result)))

  def this(result: ResultType) = this(Some(result))

  def resultInt: Option[Int] = {
    result match
      case Some(resultInt: ResultInt) => Some(resultInt.result)
      case _                          => None
  }

  def resultMessages: Option[List[Message]] = {
    result match
      case Some(resultMessage: ResultMessage)     => Some(resultMessage.result)
      case Some(resultEmptyList: ResultEmptyList) => Some(List.empty)
      case _                                      => None
  }

  def resultMap: Option[Map[Channel, Set[Message]]] = {
    result match
      case Some(resultMap: ResultMap) => Some(resultMap.result)
      case _                          => None
  }

  def resultRumor: Option[List[Rumor]] = {
    result match
      case Some(resultRumor: ResultRumor)         => Some(resultRumor.result)
      case Some(resultEmptyList: ResultEmptyList) => Some(List.empty)
      case _                                      => None
  }

  def isIntResult: Boolean =
    result match
      case Some(_: ResultInt) => true
      case _                  => false

  override def equals(obj: Any): Boolean = {
    obj match
      case that: ResultObject =>
        (this.result, that.result) match
          case (Some(a), Some(b)) => a == b
          case (None, None)       => true
          case _                  => false
      case _ => false
  }

  override def toString: String = {
    result.get match
      case ResultInt(result)     => result.toString
      case ResultMessage(result) => result.toString()
      case ResultMap(result)     => result.toString()
      case ResultRumor(result)   => result.toString()
      case ResultEmptyList()     => List.empty.toString()
  }
}
