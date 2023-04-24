package ch.epfl.pop.decentralized

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.method.Heartbeat
import ch.epfl.pop.model.objects.{Channel, DbActorNAckException, Hash}
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.graph.ErrorCodes
import ch.epfl.pop.storage.DbActor
import scala.util.{Failure, Success}
import scala.collection.immutable.HashMap
import scala.concurrent.Await

// This actor will create heartbeat when asked to and send them to the actorRef it received
case class HeartbeatGenerator(dbRef: AskableActorRef) extends Actor with ActorLogging with AskPatternConstants() {

  /** retrieves the channels and their associated message ids
    * @return
    *   a map from the channels to message ids, None when errors are encountered
    */
  private def retrieveHeartbeatContent(): Option[HashMap[Channel, Set[Hash]]] = {
    val askForChannels = dbRef ? DbActor.GetAllChannels()
    val setOfChannels: Set[Channel] = Await.ready(askForChannels, duration).value match {
      case Some(Success(DbActor.DbActorGetAllChannelsAck(set))) => set
      case Some(Failure(ex: DbActorNAckException)) =>
        log.error(s"Heartbeat generation failed with: ${ex.message}")
        return None
      case reply =>
        log.error(s"${ErrorCodes.SERVER_ERROR.id}, retrieveHeartbeatContent failed : unknown DbActor reply $reply")
        return None
    }

    var heartbeatMap: HashMap[Channel, Set[Hash]] = HashMap()
    setOfChannels.foreach(channel => {
      val askChannelData = dbRef ? DbActor.ReadChannelData(channel)
      val setOfIds: Set[Hash] = Await.ready(askChannelData, duration).value match {
        case Some(Success(DbActor.DbActorReadChannelDataAck(channelData))) => channelData.messages.toSet
        case Some(Failure(ex: DbActorNAckException)) =>
          log.error(s"Heartbeat generation failed with: ${ex.message}")
          return None
        case reply =>
          log.error(s"${ErrorCodes.SERVER_ERROR.id}, retrieveHeartbeatContent failed : unknown DbActor reply $reply")
          return None

      }
      if (setOfIds.nonEmpty)
      heartbeatMap = heartbeatMap + (channel -> setOfIds)
    })

    Some(heartbeatMap)
  }

  override def receive: Receive = {

    case Monitor.GenerateAndSendHeartbeat(actorRef) =>
      retrieveHeartbeatContent() match {
        case Some(map) =>
          actorRef ! Heartbeat(map)

        case None => /* Nothing to do, errors already logged */
      }
  }
}

object HeartbeatGenerator {

  def props(dbActorRef: AskableActorRef): Props =
    Props(new HeartbeatGenerator(dbActorRef))

}
