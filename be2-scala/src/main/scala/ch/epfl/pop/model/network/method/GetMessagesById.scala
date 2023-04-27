package ch.epfl.pop.model.network.method

import ch.epfl.pop.json.HighLevelProtocol._
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.objects.{Channel, Hash}
import spray.json._

final case class GetMessagesById(override val channelsToMessageIds: Map[Channel, Set[Hash]]) extends ParamsWithMap(channelsToMessageIds: Map[Channel, Set[Hash]])

object GetMessagesById extends Parsable {

  def apply(channelsToMessageIds: Map[Channel, Set[Hash]]): GetMessagesById =
    new GetMessagesById(channelsToMessageIds)

  override def buildFromJson(payload: String): Any = payload.parseJson.asJsObject.convertTo[GetMessagesById]
}
