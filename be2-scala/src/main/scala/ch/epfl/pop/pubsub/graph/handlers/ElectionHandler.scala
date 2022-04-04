package ch.epfl.pop.pubsub.graph.handlers

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.election.{CastVoteElection, ElectionBallotVotes, ElectionQuestion, ElectionQuestionResult, ResultElection, SetupElection}
import ch.epfl.pop.model.objects._
import ch.epfl.pop.pubsub.PubSubMediator.Propagate
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor
import ch.epfl.pop.storage.DbActor.DbActorReadAck

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

object ElectionHandler extends MessageHandler {

  def handleSetupElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    //FIXME: add election info to election channel/electionData
    val message: Message = rpcMessage.getParamsMessage.get
    val electionId: Hash = message.decodedData.get.asInstanceOf[SetupElection].id
    val electionChannel: Channel = Channel(s"${rpcMessage.getParamsChannel.channel}${Channel.CHANNEL_SEPARATOR}$electionId")

    val combined = for {
      _ <- dbActor ? DbActor.WriteAndPropagate(rpcMessage.getParamsChannel, message)
      _ <- dbActor ? DbActor.CreateChannel(electionChannel /* TODO : replace with rpcMessage.getParamsChannel ? */ , ObjectType.ELECTION)
    } yield ()

    Await.ready(combined, duration).value match {
      case Some(Success(_)) =>
        Left(rpcMessage)
      case Some(Failure(ex: DbActorNAckException)) =>
        Right(PipelineError(ex.code, s"handleSetupElection failed : ${ex.message}", rpcMessage.getId))
      case reply =>
        Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleSetupElection failed : unexpected DbActor reply '$reply'", rpcMessage.getId))
    }
  }

  def handleOpenElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    //println("open election abc")
    Await.result(ask, duration)
  }

  def handleCastVoteElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    // no need to propagate here, hence the use of dbAskWrite rpcMessage
    val ask: Future[GraphMessage] = dbAskWrite(rpcMessage)

    Await.result(ask, duration)
  }

  def handleResultElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)

    Await.result(ask, duration)
  }

  def handleEndElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    //no need to propagate the results, we only need to write the results in the db
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    val channel = rpcMessage.getParamsChannel
    val results = getResults(channel)
    // TODO get witness signatures
    val witness_signatures = rpcMessage.getParamsMessage.get.witness_signatures.map(_.signature)

    val resultElection = ResultElection(results, witness_signatures)
    val resultPropagation=dbActor ? Propagate(channel, Message(/*TODO resultElection*/ ???, ???, ???, ???, ???))
    Await.result(resultPropagation, duration)
    Await.result(ask, duration)
  }


  private def getResults(channel: Channel): List[ElectionQuestionResult] = {
    val castsVotesElections: List[CastVoteElection] = getLastVotes(channel)
    // results is a map [Question ID -> [Ballot name -> count]]
    val results = mutable.HashMap[Hash, Map[String, Int]]()
    for (castVoteElection <- castsVotesElections;
         voteElection <- castVoteElection.votes) {
      val question = voteElection.question

      // fixme : get the ballot name (from setup ?)
      val ballot = voteElection.vote match {
        case Some(List(index)) => f"<index=$index>";
        case _ => "<NOT_VOTED>"
      }
      println(f"Your ballot is $ballot !")
      if (results.contains(question)) {
        val questionresult = results(question)
        if (questionresult.contains(ballot))
          results.update(question, questionresult.updated(ballot, questionresult(ballot) + 1))
        else
          results.update(question, questionresult.updated(ballot, 1))
      }
      else
        results.update(question, Map(ballot -> 1))
    }
    println(results)
    results.map(tuple => {
      val (qid, ballot2count) = tuple
      ElectionQuestionResult(qid, ballot2count.map(tuple => ElectionBallotVotes(tuple._1, tuple._2)).toList)
    }).toList
  }


  /**
   * Read every castvote in the channel and keep the last per attendee
   *
   * @param channel : the election channel
   * @return the final vote for each attendee that has voted
   */
  private def getLastVotes(channel: Channel): List[CastVoteElection] =
    Await.ready(dbActor ? DbActor.ReadChannelData(channel), duration).value match {
      case Some(Success(DbActor.DbActorReadChannelDataAck(channelData))) =>
        val messages: List[Hash] = channelData.messages
        val lastVotes: mutable.HashMap[PublicKey, CastVoteElection] = new mutable.HashMap()
        for (messageIdHash <- messages) {
          val dbAnswer =
            Await.ready(dbActor ? DbActor.Read(channel = channel, messageId = messageIdHash), duration).value.get match {
              case Success(value) => value
              case _ => None
            }
          dbAnswer match {
            case DbActorReadAck(Some(message)) =>
              val sender = message.sender
              // FIXME : the message contains the correct base64-encoded json in data, but message.decodedData return None
              /*message.decodedData match {
                case Some(castVote: CastVoteElection) =>
                  if (!lastVotes.contains(sender) || !(castVote.created_at < lastVotes(sender).created_at))
                    lastVotes.update(sender, castVote)
                case _ =>
                  ()
              }*/
              try {
                val castVote: CastVoteElection = CastVoteElection.buildFromJson(message.data.decodeToString())
                if (!lastVotes.contains(sender) || !(castVote.created_at < lastVotes(sender).created_at))
                  lastVotes.update(sender, castVote)
              } catch {
                case exception: Throwable => print(exception)
              }
            case _ =>
          }
        }
        println(lastVotes)
        lastVotes.values.toList
      case _ => Nil
    }

  private def compareResults(messages: List[Hash], checkHash: Hash): Boolean =
    Hash(Base64Data(messages.map(_.toString).sorted.foldLeft("")(_ + _))) == checkHash
}
