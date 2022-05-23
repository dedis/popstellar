package ch.epfl.pop.model.objects

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.election.{CastVoteElection, SetupElection}
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.storage.DbActor
import ch.epfl.pop.storage.DbActor.{DbActorReadAck, DbActorReadChannelDataAck}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

object ElectionChannel {
  implicit class ElectionChannel(channel: Channel) extends AskPatternConstants {

    /**
     *
     * @param dbActor AskableActorRef
     * @tparam T type of data to extract from the election channel
     * @return Future of a list of tuple containing the message and the data extracted
     */
    def extractMessages[T: Manifest](dbActor: AskableActorRef = DbActor.getInstance): Future[List[(Message, T)]] = {
      for {
        DbActor.DbActorReadChannelDataAck(channelData) <- dbActor ? DbActor.ReadChannelData(channel)
        result <- Future.traverse(channelData.messages) { messageHash =>
          for {
            DbActorReadAck(Some(message)) <- dbActor ? DbActor.Read(channel, messageHash)
          } yield message.decodedData match {
            case Some(t: T) => Some((message, t))
            case _ => None
          }
        }
      } yield result.flatten
    }

    /** returns the SetupElection message from the channel
     *
     * @param dbActor the AskableActorRef we use (by default the main DbActor, obtained through getInstance)
     * @return the SetupElection message
     */
    def getSetupMessage(dbActor: AskableActorRef = DbActor.getInstance): Future[SetupElection] =
      extractMessages[SetupElection](dbActor).map(_.head._2)

    /**
     * Read every castvote in the channel and keep the last per attendee
     *
     * @param dbActor the AskableActorRef we use (by default the main DbActor, obtained through getInstance)
     * @return the final vote for each attendee that has voted
     */
    def getLastVotes(dbActor: AskableActorRef = DbActor.getInstance): Future[List[(Message, CastVoteElection)]] = {
      extractMessages[CastVoteElection](dbActor).map(votes => {
        val attendeeIdToLastVote = mutable.Map[PublicKey, (Message, CastVoteElection)]()
        for ((message, castvote) <- votes) {
          if (!attendeeIdToLastVote.contains(message.sender) ||
            attendeeIdToLastVote(message.sender)._2.created_at < castvote.created_at) {
            attendeeIdToLastVote.update(message.sender, (message, castvote))
          }
        }
        attendeeIdToLastVote.values.toList
      })
    }
  }
}
