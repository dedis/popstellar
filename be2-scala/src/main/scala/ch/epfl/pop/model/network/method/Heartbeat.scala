package ch.epfl.pop.model.network.method

import ch.epfl.pop.json.HighLevelProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.objects.{Channel, Hash}
import spray.json._

final case class Heartbeat(override val channelsToMessageIds: Map[Channel, Set[Hash]]) extends ParamsWithMap(channelsToMessageIds: Map[Channel, Set[Hash]])

object Heartbeat extends Parsable {

  def apply(channelsToMessageIds: Map[Channel, Set[Hash]]): Heartbeat =
    new Heartbeat(channelsToMessageIds)

  override def buildFromJson(payload: String): Any = payload.parseJson.asJsObject.convertTo[Heartbeat]
}
