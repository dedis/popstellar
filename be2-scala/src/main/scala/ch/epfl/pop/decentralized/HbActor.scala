package ch.epfl.pop.decentralized

import akka.actor.Actor
import ch.epfl.pop.decentralized.HbActor._
import ch.epfl.pop.model.objects.{Channel, Hash}
import ch.epfl.pop.storage.DbActor
import akka.pattern.AskableActorRef

import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask
import ch.epfl.pop.pubsub.AskPatternConstants

import scala.collection.mutable
import scala.concurrent.Await

// note that the Await method is strongly discouraged.
final case class HbActor(
    private val dbRef: AskableActorRef
) extends Actor with AskPatternConstants {

  implicit val timeout: Timeout = 3.seconds

  /** @param setOfChannels
    *   the set of channels that are recorded in the DB
    * @return
    *   the map that maps a given channel to a list of message ids that have been sent over this channel.
    */
  private def retrieveHeartbeatContent(setOfChannels: Set[String]): mutable.HashMap[String, List[Hash]] = {
    var res: mutable.HashMap[String, List[Hash]] = mutable.HashMap()
    setOfChannels.foreach(channel => {
      val ask = dbRef ? DbActor.ReadChannelData(Channel(channel))
      val answer = Await.result(ask, duration)
      val listOfIds: List[Hash] = answer.asInstanceOf[DbActor.DbActorReadChannelDataAck].channelData.messages
      res += (channel -> listOfIds)
    })
    res
  }

  /** @param selfHeartbeat
    *   the map that maps a given channel to the list of message ids that have been received by the server.
    * @param receivedHeartbeat
    *   the map that maps a given channel to the list of message ids that have been received by the server sending the heartbeat.
    * @return
    *   the missing message ids.
    */
  private def retrieveMissingMessageIds(selfHeartbeat: mutable.HashMap[String, List[Hash]], receivedHeartbeat: mutable.HashMap[String, List[Hash]]): mutable.HashMap[String, List[Hash]] = {
    val res: mutable.HashMap[String, List[Hash]] = mutable.HashMap()
    receivedHeartbeat.keys.foreach(channel => {
      if (selfHeartbeat.contains(channel)) {
        res += (channel -> receivedHeartbeat.get(channel).get.filter(id => !selfHeartbeat.get(channel).get.contains(id)))
      }
    })
    res
  }

  private def askDbForHeartBeat(): mutable.HashMap[String, List[Hash]] = {
    val ask = dbRef ? DbActor.GetSetOfChannels()
    val answer = Await.result(ask, duration)
    val setOfChannels: Set[String] = answer.asInstanceOf[DbActor.DbActorGetSetOfChannelsAck].channels
    val heartbeatContent = retrieveHeartbeatContent(setOfChannels)
    heartbeatContent
  }

  override def receive: Receive = {
    case RetrieveHeartbeat() =>
      val heartbeatContent = askDbForHeartBeat()
      sender() ! HbActorRetrieveHeartbeatAck(heartbeatContent)
    case CompareHeartbeat(receivedHeartbeat) =>
      val selfHeartbeat = askDbForHeartBeat()
      val missingIds = retrieveMissingMessageIds(selfHeartbeat, receivedHeartbeat)
      sender() ! HbActorCompareHeartBeatAck(missingIds)
  }
}

object HbActor {

  // DbActor Events correspond to messages the actor may receive
  sealed trait Event

  /** Request to retrieve the Map of channels and message ids associated to these channels.
    */
  final case class RetrieveHeartbeat() extends Event

  /** Request to compare the received Heartbeat with the set of message ids stored in the server's DB
    * @param receivedHeartbeat
    *   The received Heartbeat's content.
    */

  final case class CompareHeartbeat(receivedHeartbeat: mutable.HashMap[String, List[Hash]]) extends Event

  // HbActor HbActorMessage correspond to messages the actor may emit
  sealed trait HbActorMessage

  /** Response for a [[RetrieveHeartBeat]] request. Receiving a [[HbActorRetrieveHeartBeatAck]] means that the retrieving of the HeartbeatContent was successfull.
    * @param heartbeatContent
    *   the content of the Heartbeat the server should send.
    */
  final case class HbActorRetrieveHeartbeatAck(heartbeatContent: mutable.HashMap[String, List[Hash]]) extends HbActorMessage

  final case class HbActorCompareHeartBeatAck(missingIds: mutable.HashMap[String, List[Hash]]) extends HbActorMessage
}
