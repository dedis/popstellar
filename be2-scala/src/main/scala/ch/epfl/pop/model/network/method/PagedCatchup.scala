package ch.epfl.pop.model.network.method

import ch.epfl.pop.json.HighLevelProtocol.PagedCatchupFormat
import ch.epfl.pop.model.network.Parsable
import ch.epfl.pop.model.objects.{Channel}
import spray.json._

final case class PagedCatchup(channel: Channel, numberOfMessages: Int, beforeMessageID: Option[String]) extends Params {

  override def hasChannel: Boolean = true

  override def hasMessage: Boolean = false

}

object PagedCatchup extends Parsable {

  def apply(channel: Channel, numberOfMessages: Int, beforeMessageID: Option[String]): PagedCatchup = {
    new PagedCatchup(channel, numberOfMessages, beforeMessageID)
  }

  override def buildFromJson(payload: String): PagedCatchup = payload.parseJson.asJsObject.convertTo[PagedCatchup]

}
