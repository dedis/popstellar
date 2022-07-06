package ch.epfl.pop.pubsub.graph.validators

import ch.epfl.pop.model.objects.Channel

import scala.util.{Success, Try}

trait MethodContentValidator extends ContentValidator {

  def validateChannel(channel: Channel): Boolean = channel match {
    case _ if channel.isRootChannel => true
    case _ if !channel.isSubChannel => false
    case _ => Try(channel.decodeChannelLaoId) match {
        case Success(_) => true
        case _          => false
      }
  }
}
