package ch.epfl.pop.decentralized

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.{Actor, ActorSystem, Status}
import ch.epfl.pop.decentralized.HbActor._
import ch.epfl.pop.model.objects.{Channel, Hash, HbActorNAckException}
import ch.epfl.pop.pubsub.graph.ErrorCodes
import ch.epfl.pop.storage.DbActor
import akka.pattern.AskableActorRef
import ch.epfl.pop.storage.DbActor.GetSetOfChannels

import scala.concurrent.duration._
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import akka.pattern.ask
import ch.epfl.pop.pubsub.AskPatternConstants

import scala.util.{Failure, Success, Try}
import scala.collection.mutable.{HashMap, SortedSet}
import scala.concurrent.Await

final case class HbActor(
    private val dbRef : ActorRef[DbActor]
) extends Actor {

  implicit val timeout: Timeout = 3.seconds
  val duration : Duration = 3.seconds


  override def receive: Receive = {
    case RetrieveHeartBeat() =>
      val ask = dbRef ? DbActor.GetSetOfChannels()
      val answer = Await.result(ask, duration)
  }
}

object HbActor {

  // DbActor Events correspond to messages the actor may receive
  sealed trait Event

  /**
   * Request to retrieve the Map of channels and message ids associated to these channels.
   */
  final case class RetrieveHeartBeat() extends Event

  // HbActor HbActorMessage correspond to messages the actor may emit
  sealed trait HbActorMessage

  /**
   * Response for a [[RetrieveHeartBeat]] request.
   * Receiving a [[HbActorRetrieveHeartBeatAck]] means that the retrieving of the HeartBeatContent was successfull.
   * @param heartBeatContent the content of the HeartBeat the server should send.
   */
  final case class HbActorRetrieveHeartBeatAck(heartBeatContent : Map[Channel, List[String]])
}
