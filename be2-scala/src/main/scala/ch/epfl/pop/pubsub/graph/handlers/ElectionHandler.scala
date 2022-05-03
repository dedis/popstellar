package ch.epfl.pop.pubsub.graph.handlers

import akka.pattern.AskableActorRef
import ch.epfl.pop.json.MessageDataProtocol.KeyElectionFormat
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.election.{KeyElection, SetupElection}
import ch.epfl.pop.model.objects.{Base64Data, Channel, DbActorNAckException, Hash, PublicKey, Signature}
import ch.epfl.pop.pubsub.graph.handlers.LaoHandler.{dbActor, duration}
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

/**
 * ElectionHandler object uses the db instance from the MessageHandler
 */
object ElectionHandler extends MessageHandler {
  final lazy val handlerInstance = new ElectionHandler(super.dbActor)

  def handleSetupElection(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleSetupElection(rpcMessage)

  def handleOpenElection(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleOpenElection(rpcMessage)

  def handleCastVoteElection(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleCastVoteElection(rpcMessage)

  def handleResultElection(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleResultElection(rpcMessage)

  def handleEndElection(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleEndElection(rpcMessage)
}

class ElectionHandler(dbRef: => AskableActorRef) extends MessageHandler {

  /**
   *
   * Overrides default DbActor with provided parameter
   */
  override final val dbActor: AskableActorRef = dbRef

  def handleSetupElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    //FIXME: add election info to election channel/electionData
    val message: Message = rpcMessage.getParamsMessage.get
    val laoChannel: Channel = rpcMessage.getParamsChannel
    val electionId: Hash = message.decodedData.get.asInstanceOf[SetupElection].id
    val electionChannel: Channel = Channel(s"${laoChannel.channel}${Channel.CHANNEL_SEPARATOR}$electionId")

    //need to write and propagate the election message
    val combined = for {
      _ <- dbActor ? DbActor.WriteAndPropagate(laoChannel, message)
      _ <- dbActor ? DbActor.CreateChannel(electionChannel, ObjectType.ELECTION)
    } yield ()

    Await.ready(combined, duration).value match {
      case Some(Success(_)) =>
        val laoId: Hash = rpcMessage.extractLaoId
        //how to generate the publickey ??
        val electionKey: KeyElection = KeyElection(laoId, electionId, PublicKey(Base64Data("")))
        val broadcastKey: Base64Data = Base64Data.encode(KeyElectionFormat.write(electionKey).toString)

        val askLaoData = dbActor ? DbActor.ReadLaoData(laoChannel)

        Await.ready(askLaoData, duration).value match {
          case Some(Success(DbActor.DbActorReadLaoDataAck(laoData))) =>
            val broadcastSignature: Signature = laoData.privateKey.signData(broadcastKey)
            val broadcastId: Hash = Hash.fromStrings(broadcastKey.toString, broadcastSignature.toString)
            val broadcastMessage: Message = Message(broadcastKey, laoData.publicKey, broadcastSignature, broadcastId, List.empty)

            val askWritePropagate = dbActor ? DbActor.WriteAndPropagate(electionChannel, broadcastMessage)
            Await.ready(askWritePropagate, duration).value.get match {
              case Success(_) => Left(rpcMessage)
              case Failure(ex: DbActorNAckException) => Right(PipelineError(ex.code, s"broadcasting election key failed : ${ex.message}", rpcMessage.getId))
              case reply => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"broadcasting election key failed : unknown DbActor reply $reply", rpcMessage.getId))
            }
          case Some(Failure(ex: DbActorNAckException)) => Right(PipelineError(ex.code, s"Reading the LaoData in handleSetupElection failed : ${ex.message}", rpcMessage.getId))
          case reply => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleSetupElection failed : unknown DbActor reply $reply", rpcMessage.getId))
        }
      case Some(Failure(ex: DbActorNAckException)) => Right(PipelineError(ex.code, s"handleSetupElection failed : ${ex.message}", rpcMessage.getId))
      case reply => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleSetupElection failed : unexpected DbActor reply '$reply'", rpcMessage.getId))
    }
  }

  def handleOpenElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    //checks first if the election is created (i.e. if the channel election exists)
    val askChannelExist = dbActor ? DbActor.ChannelExists(rpcMessage.getParamsChannel)
    Await.ready(askChannelExist, duration).value.get match {
      case Success(_) =>
        val askWritePropagate: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
        Await.result(askWritePropagate, duration)
      case Failure(ex: DbActorNAckException) => Right(PipelineError(ex.code, s"handleOpenElection failed : ${ex.message}", rpcMessage.getId))
      case reply => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleOpenElection failed : unknown DbActor reply $reply", rpcMessage.getId))
    }
  }

  def handleCastVoteElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    // no need to propagate here, hence the use of dbAskWrite
    val ask: Future[GraphMessage] = dbAskWrite(rpcMessage)
    Await.result(ask, duration)
  }

   def handleResultElection(rpcMessage: JsonRpcRequest): GraphMessage = Right(
    PipelineError(ErrorCodes.SERVER_ERROR.id, "NOT IMPLEMENTED: ElectionHandler cannot handle ResultElection messages yet", rpcMessage.id)
  )

  def handleEndElection(rpcMessage: JsonRpcRequest): GraphMessage = Right(
    PipelineError(ErrorCodes.SERVER_ERROR.id, "NOT IMPLEMENTED: ElectionHandler cannot handle EndElection messages yet", rpcMessage.id)
  )
}
