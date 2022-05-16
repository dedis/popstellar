package ch.epfl.pop.pubsub.graph.handlers

import akka.pattern.AskableActorRef
import ch.epfl.pop.json.MessageDataProtocol.KeyElectionFormat
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.election.{KeyElection, SetupElection}
import ch.epfl.pop.model.objects.{Base64Data, Channel, DbActorNAckException, Hash, KeyPair}
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

  def handleKeyElection(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleKeyElection(rpcMessage)
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
    val data: SetupElection = message.decodedData.get.asInstanceOf[SetupElection]
    val electionId: Hash = data.id
    val electionChannel: Channel = Channel(s"${rpcMessage.getParamsChannel.channel}${Channel.CHANNEL_SEPARATOR}$electionId")

    //need to write and propagate the election message
    val combined = for {
      _ <- dbActor ? DbActor.WriteAndPropagate(rpcMessage.getParamsChannel, message)
      _ <- dbActor ? DbActor.CreateChannel(electionChannel, ObjectType.ELECTION)
      _ <- dbActor ? DbActor.WriteAndPropagate(electionChannel, message)
    } yield ()

    Await.ready(combined, duration).value match {
      case Some(Success(_)) =>
        //generating a key pair in case the version is secret-ballot, to send then the public key to the frontend
        data.version match {
          case "OPEN_BALLOT" => Left(rpcMessage)
          case "SECRET_BALLOT" =>
            val keyPair = KeyPair()
            val elecCombined = for {
              _ <- dbActor ? DbActor.CreateElectionData(electionId, keyPair)
              electionData <- dbActor ? DbActor.ReadElectionData(electionId)
            } yield electionData

            Await.ready(elecCombined, duration).value match {
              case Some(Success(_)) =>
                val keyElection: KeyElection = KeyElection(electionId, keyPair.publicKey)
                val broadcastKey: Base64Data = Base64Data.encode(KeyElectionFormat.write(keyElection).toString)
                dbBroadcast(rpcMessage, rpcMessage.getParamsChannel, broadcastKey, electionChannel)
              case Some(Failure(ex: DbActorNAckException)) => Right(PipelineError(ex.code, s"handleSetupElection failed : ${ex.message}", rpcMessage.getId))
              case reply => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleSetupElection failed : unexpected DbActor reply '$reply'", rpcMessage.getId))
            }
        }
      case Some(Failure(ex: DbActorNAckException)) => Right(PipelineError(ex.code, s"handleSetupElection failed : ${ex.message}", rpcMessage.getId))
      case reply => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleSetupElection failed : unexpected DbActor reply '$reply'", rpcMessage.getId))
    }
  }

  def handleKeyElection(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration)
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
