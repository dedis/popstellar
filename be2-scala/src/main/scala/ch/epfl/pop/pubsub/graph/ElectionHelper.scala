package ch.epfl.pop.pubsub.graph

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.election._
import ch.epfl.pop.model.objects._
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.storage.DbActor
import ch.epfl.pop.storage.DbActor.DbActorReadAck

import scala.collection.mutable
import scala.concurrent.Await
import scala.util.Success

/**
 * functions that are used in handlers and validators of election
 */
object ElectionHelper extends AskPatternConstants {

  /** returns the SetupElection message from the channel
   *
   * @param electionChannel the election channel
   * @param dbActor         the AskableActorRef we use (by default the main DbActor, obtained through getInstance)
   * @return the SetupElection message
   */
  def getSetupMessage(electionChannel: Channel, dbActor: AskableActorRef = DbActor.getInstance): SetupElection =
    getAllMessage[SetupElection](electionChannel, dbActor).head._2

  def getAllMessage[T: Manifest](electionChannel: Channel, dbActor: AskableActorRef = DbActor.getInstance): List[(Message, T)] = {
    var result: List[(Message, T)] = Nil
    Await.ready(dbActor ? DbActor.ReadChannelData(electionChannel), duration).value match {
      case Some(Success(DbActor.DbActorReadChannelDataAck(channelData))) =>
        for (message <- channelData.messages) {
          (Await.ready(dbActor ? DbActor.Read(electionChannel, message), duration).value match {
            case Some(Success(value)) => value
            case _ => None
          }) match {
            case DbActorReadAck(Some(message)) =>
              message.decodedData match {
                case Some(t: T) =>
                  result = (message, t) :: result
                case _ =>
              }
          }
        }
      case _ =>
    }
    result
  }

  /**
   * Read every castvote in the channel and keep the last per attendee
   *
   * @param electionChannel : the election channel
   * @param dbActor         the AskableActorRef we use (by default the main DbActor, obtained through getInstance)
   * @return the final vote for each attendee that has voted
   */
  def getLastVotes(electionChannel: Channel, dbActor: AskableActorRef = DbActor.getInstance): List[(Message, CastVoteElection)] = {
    val votes = getAllMessage[CastVoteElection](electionChannel, dbActor)
    val attendeeId2lastVote = mutable.Map[PublicKey, (Message, CastVoteElection)]()
    for ((message, castvote) <- votes
         if !attendeeId2lastVote.contains(message.sender) ||
           attendeeId2lastVote(message.sender)._2.created_at < castvote.created_at)
      attendeeId2lastVote.update(message.sender, (message, castvote))
    attendeeId2lastVote.values.toList
  }
}
