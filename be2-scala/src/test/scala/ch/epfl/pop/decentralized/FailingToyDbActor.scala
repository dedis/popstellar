package ch.epfl.pop.decentralized

import akka.actor.Actor
import ch.epfl.pop.model.objects.{Channel, DbActorNAckException}
import ch.epfl.pop.pubsub.graph.ErrorCodes.SERVER_ERROR
import ch.epfl.pop.storage.DbActor

class FailingToyDbActor extends Actor {
  override def receive: Receive = {
    case _ => DbActorNAckException(0, "")
  }

}
