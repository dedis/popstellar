package ch.epfl.pop.decentralized

import akka.actor.Actor
import ch.epfl.pop.model.objects.DbActorNAckException

class FailingToyDbActor extends Actor {
  override def receive: Receive = {
    case _ =>
      sender() ! DbActorNAckException(0, "")
  }

}
