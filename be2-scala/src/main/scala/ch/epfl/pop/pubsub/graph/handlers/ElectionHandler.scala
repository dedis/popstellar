package ch.epfl.pop.pubsub.graph.handlers

import ch.epfl.pop.json.MessageDataProtocol.resultElectionFormat
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.election.{CastVoteElection, ElectionBallotVotes, ElectionQuestionResult, ResultElection, SetupElection}
import ch.epfl.pop.model.objects.{Base64Data, Channel, DbActorNAckException, Hash, PublicKey}
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor
import ch.epfl.pop.storage.DbActor.DbActorReadAck
import spray.json.enrichAny

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
import scala.collection.mutable

object ElectionHandler extends MessageHandler {

  def handleSetupElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    //FIXME: add election info to election channel/electionData
    val message: Message = rpcMessage.getParamsMessage.get
    val electionId: Hash = message.decodedData.get.asInstanceOf[SetupElection].id
    val electionChannel: Channel = Channel(s"${rpcMessage.getParamsChannel.channel}${Channel.CHANNEL_SEPARATOR}$electionId")

    val combined = for {
      _ <- dbActor ? DbActor.CreateChannel(electionChannel /* TODO : replace with rpcMessage.getParamsChannel ? */ , ObjectType.ELECTION)
      _ <- dbActor ? DbActor.WriteAndPropagate(rpcMessage.getParamsChannel, message)
      _ <- dbActor ? DbActor.WriteAndPropagate(electionChannel, message)
    } yield ()

    Await.ready(combined, duration).value match {
      case Some(Success(_)) => Left(rpcMessage)
      case Some(Failure(ex: DbActorNAckException)) => Right(PipelineError(ex.code, s"handleSetupElection failed : ${ex.message}", rpcMessage.getId))
      case reply => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleSetupElection failed : unexpected DbActor reply '$reply'", rpcMessage.getId))
    }
  }

  def handleCastVoteElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    // no need to propagate here, hence the use of dbAskWrite
    val ask: Future[GraphMessage] = dbAskWrite(rpcMessage)
    Await.result(ask, duration)
  }

  def handleResultElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)

    Await.result(ask, duration)
  }

  def handleEndElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    //no need to propagate the results, we only need to write the results in the db
    Await.ready(dbAskWritePropagate(rpcMessage), duration).value match {
      case Some(Success(_)) =>
        val electionChannel = rpcMessage.getParamsChannel
        val results = getResults(electionChannel)
        val witness_signatures = rpcMessage.getParamsMessage match {
          case Some(it) => it.witness_signatures.map(_.signature)
          case _ => Nil
        }
        val resultElection = ResultElection(results, witness_signatures)
        val data = Base64Data.encode(resultElection.toJson.toString)
        // TODO create the signature
        val askLaoData = dbActor ? DbActor.ReadLaoData(rpcMessage.getParamsChannel)
        Await.ready(askLaoData, duration).value match {
          case Some(Success(DbActor.DbActorReadLaoDataAck(laoData))) =>
            val signature = laoData.privateKey.signData(data)
            val sender = laoData.publicKey
            val msgId = Hash.fromStrings(data.toString, signature.toString)
            val message = Message(data,
              sender,
              signature,
              msgId,
              List.empty, Some(resultElection))
            val propagation = dbActor ? DbActor.WriteAndPropagate(electionChannel, message)
            Await.ready(propagation, duration).value match {
              case Some(Success(_)) =>
                Left(rpcMessage)
              case Some(Failure(ex: DbActorNAckException)) =>
                Right(PipelineError(ex.code, s"handleCloseElection failed : ${
                  ex.message
                }", rpcMessage.getId))
              case reply =>
                Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleCloseElection failed : unexpected DbActor reply '$reply'", rpcMessage.getId))
            }
          case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleCloseElection failed : couldn't read LAO data", rpcMessage.getId))
        }
      case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleCloseElection failed : askWritePropagate failed", rpcMessage.getId))
    }
  }


  private def getResults(electionChannel: Channel): List[ElectionQuestionResult] = {
    val castsVotesElections: List[CastVoteElection] = getLastVotes(electionChannel)
    val question2Ballots = getSetupMessage(electionChannel).questions.map(question => question.id -> question.ballot_options).toMap
    // results is a map [Question ID -> [Ballot name -> count]]
    val r = question2Ballots.keys.map(question => question -> question2Ballots(question).map(_ -> 0).toMap).toMap
    val results = mutable.HashMap[Hash, Map[String, Int]]() ++ r
    for (castVoteElection <- castsVotesElections;
         voteElection <- castVoteElection.votes) {
      val question = voteElection.question //.asInstanceOf[ElectionQuestion]
      val ballots = question2Ballots(question)
      val ballot = voteElection.vote match {
        case Some(List(index)) => ballots.toArray.apply(index)
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



  def getSetupMessage(electionChannel: Channel): SetupElection = {
    var result: SetupElection = null
    Await.ready(dbActor ? DbActor.ReadChannelData(electionChannel), duration).value match {
      case Some(Success(DbActor.DbActorReadChannelDataAck(channelData))) =>
        for (message <- channelData.messages) {
          (Await.ready(dbActor ? DbActor.Read(electionChannel, message), duration).value match {
            case Some(Success(value)) => value
            case _ => None
          }) match {
            case DbActorReadAck(Some(message)) =>
              try {
                result = SetupElection.buildFromJson(message.data.decodeToString())
              } catch {
                case exception: Throwable => print(exception)
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
   * @return the final vote for each attendee that has voted
   */
  private def getLastVotes(electionChannel: Channel): List[CastVoteElection] =
    Await.ready(dbActor ? DbActor.ReadChannelData(electionChannel), duration).value match {
      case Some(Success(DbActor.DbActorReadChannelDataAck(channelData))) =>
        val messages: List[Hash] = channelData.messages
        val lastVotes: mutable.HashMap[PublicKey, CastVoteElection] = new mutable.HashMap()
        for (messageIdHash <- messages) {
          val dbAnswer =
            Await.ready(dbActor ? DbActor.Read(channel = electionChannel, messageId = messageIdHash), duration).value match {
              case Some(Success(value)) => value
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

}
