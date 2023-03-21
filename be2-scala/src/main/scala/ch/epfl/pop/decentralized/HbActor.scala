package ch.epfl.pop.decentralized

import akka.actor.{Actor, Status}
import ch.epfl.pop.decentralized.HbActor._
import ch.epfl.pop.model.objects.{Channel, Hash, HbActorNAckException}
import ch.epfl.pop.pubsub.graph.ErrorCodes
import scala.util.{Failure, Success, Try}
import scala.collection.mutable.{HashMap, SortedSet}

final case class HbActor(
    private val seenMessages: HashMap[String, List[Hash]] = new HashMap[String, List[Hash]](),
    private val subscribedChannels: Set[String] = Set()
) extends Actor {

  private def write(channel: String, messageId: Hash): Option[Hash] = {
    seenMessages.get(channel) match {
      case Some(list) =>
        if (list.contains(messageId)) {
          throw HbActorNAckException(ErrorCodes.SERVER_ERROR.id, s"%$messageId is already stored.")
        } else {
          list.::(messageId) // do not forget to fix this.
          Some(messageId)
        }
      case _ =>
        seenMessages.addOne((channel, List(messageId)))
        Some(messageId)
    }
  }

  /** @param receivedHeartBeat
    *   The received heartBeat from another server.
    * @return
    *   Some(The missing messages from the channels the server is subscribed to) None in the case where the server does not have any message to catch up.
    */
  private def extractMissingMessages(receivedHeartBeat: Map[String, List[Hash]]): Option[Map[String, List[Hash]]] = {
    val channels = receivedHeartBeat.keys.toList.filter(channel => subscribedChannels.contains(channel)) // je récupère les channels que j'ai en commun avec le serveur qui m'envoie le heartBeat.
    val result = new HashMap[String, List[Hash]]()
    channels.foreach(channel =>
      result.addOne((channel, extractMissingIdsPerChannel(channel, receivedHeartBeat.get(channel).getOrElse(List())).getOrElse(List()))) // if there is no missing id per channel, we simply add (channel, List()) to the map.
    )
    result.filter(elem => !elem._2.isEmpty) // we remove the channels we have no message to catch up from.
    if (!result.isEmpty)
      return Some(result.toMap)
    None

  }

  /** @param channel
    *   the name of the channel we want to know if we missed any message from it.
    * @param receivedIds
    *   the received messageIds for the channel <channel>.
    * @return
    *   Some(List(missing messageIds)) if we missed any message with respect to this channel. None if we didn't miss anything.
    */
  private def extractMissingIdsPerChannel(channel: String, receivedIds: List[Hash]): Option[List[Hash]] = {
    if (receivedIds.isEmpty) { // can receivedIds be empty ?
    return None
    }
    val missingIds = receivedIds.filter(id => !seenMessages.get(channel).contains(id)) // we do not handle the case where seenMessages.get(channel) is not defined because the method is only called for channels we are subscribed to
    if (!missingIds.isEmpty)
      return Some(missingIds)
    None
  }
  override def receive: Receive = {
    case writeMessage(channel, messageId) =>
      Try(write(channel, messageId)) match {
        case Success(messageId) => sender() ! HbActorAck()
        case failure            => failure.recover(Status.Failure(_))
      }
    case subscribed(channel) =>
      subscribedChannels.+(channel)
      sender() ! HbActorAck()
    case getMissingMessages(receivedHeartBeat) =>
      val missingMessages = extractMissingMessages(receivedHeartBeat)
      sender() ! MissingMessagesResponse(missingMessages)
    case retrieveMessagesForHeartBeat() =>
      sender() ! retrieveMessagesResponse(seenMessages)
  }
}

object HbActor {

  // DbActor Events correspond to messages the actor may receive
  sealed trait Event

  /** Request to record seeing a message.
    *
    * @param channel
    *   the channel into which the message was sent.
    * @param messageId
    *   the id of the message.
    */
  final case class writeMessage(channel: String, messageId: Hash) extends Event

  /** Request to send back the all the messages seen until now to the higher level actor so that he can send a heartbeat in his turn.
    */
  final case class retrieveMessagesForHeartBeat() extends Event

  /** Request to compare the messages the server has seen with messages another server has seen
    *
    * @param receivedHeartBeat
    *   The messages seen by the other server.
    */
  final case class getMissingMessages(receivedHeartBeat: Map[String, List[Hash]])

  /** Message sent by higher level actor to inform what channels the server is interested in.
    *
    * @param channel
    *   The channel the server is interested in.
    */
  final case class subscribed(channel: String)

  // DbActor DbActorMessage correspond to messages the actor may emit
  sealed trait DbActorMessage

  /** Response for a general db actor ACK
    */
  final case class HbActorAck() extends DbActorMessage


  /** Response for a getMissingMessages request.
    * @param response
    *   the missing messages.
    */
  final case class MissingMessagesResponse(response: Option[Map[String, List[Hash]]]) extends DbActorMessage

  final case class retrieveMessagesResponse(response: HashMap[String, List[Hash]]) extends DbActorMessage

}
