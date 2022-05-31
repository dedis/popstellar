package ch.epfl.pop.model.objects

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.election.{CastVoteElection, SetupElection}
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.storage.DbActor

import java.nio.ByteBuffer
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ElectionChannel {
  implicit class ElectionChannelExtensionMethods(channel: Channel) extends AskPatternConstants {

    /**
     *
     * @param dbActor AskableActorRef
     * @tparam T type of data to extract from the election channel
     * @return Future of a list of tuple containing the message and the data extracted
     */
    def extractMessages[T: Manifest](dbActor: AskableActorRef = DbActor.getInstance): Future[List[(Message, T)]] = {
      for {
        DbActor.DbActorCatchupAck(messages) <- dbActor ? DbActor.Catchup(channel)
        result <- Future.traverse(messages.flatMap(message =>
          message.decodedData match {
            case Some(t: T) => Some((message, t))
            case _ => None
          })
        ) { message => Future(message) }
      } yield result
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
      extractMessages[CastVoteElection](dbActor).map{votes =>
        val attendeeIdToLastVote = mutable.Map[PublicKey, (Message, CastVoteElection)]()
        for ((message, castvote) <- votes) {
          if (!attendeeIdToLastVote.contains(message.sender) || attendeeIdToLastVote(message.sender)._2.created_at < castvote.created_at) {
            attendeeIdToLastVote.update(message.sender, (message, castvote))
          }
        }
        attendeeIdToLastVote.values.toList
      }
    }

    /**
     * return index of the vote
     *
     * @param electionData the electiondata containing keys if need to decrypt
     * @param vote         None if not voted, Some(Left(Int)) for non open ballots Right(Base64Data) for encrypted vote
     * @return The index of the ballot, -1 if not voted
     */
    def getVoteIndex(electionData: ElectionData, vote: Option[Either[Int, Base64Data]]): Int =
      vote match {
        case Some(Left(index)) =>
          index
        case Some(Right(encryptedVote)) =>
          ByteBuffer.wrap(Array[Byte](0, 0).concat(electionData.keyPair.decrypt(encryptedVote).decode())).getInt
        case _ =>
          -1
      }
  }
}
