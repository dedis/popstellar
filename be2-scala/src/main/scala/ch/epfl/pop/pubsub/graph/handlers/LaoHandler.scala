package ch.epfl.pop.pubsub.graph.handlers

import ch.epfl.pop.config.RuntimeEnvironment.ownClientAddress
import ch.epfl.pop.json.MessageDataProtocol.GreetLaoFormat
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.lao.{CreateLao, GreetLao, StateLao}
import ch.epfl.pop.model.objects.{Channel, DbActorNAckException, Hash}
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

case object LaoHandler extends MessageHandler {

  def handleCreateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask =
      for {
        (_, message, somedata) <- extractParameters[CreateLao](rpcMessage, s"Unable to handle lao message $rpcMessage. Not a Publish/Broadcast message")
        data: CreateLao = somedata.get
        // we are using the lao id instead of the message_id at lao creation
        laoChannel: Channel = Channel(s"${Channel.ROOT_CHANNEL_PREFIX}${data.id}")
        coinChannel: Channel = Channel(s"$laoChannel${Channel.COIN_CHANNEL_PREFIX}")
        socialChannel: Channel = Channel(s"$laoChannel${Channel.SOCIAL_MEDIA_CHIRPS_PREFIX}")
        reactionChannel: Channel = Channel(s"$laoChannel${Channel.REACTIONS_CHANNEL_PREFIX}")
        // we get access to the canonical address of the server
        address: Option[String] = Some(ownClientAddress)

        // check whether the lao already exists in db
        _ <- dbActor ? DbActor.AssertChannelMissing(laoChannel)
        // create lao channels
        _ <- dbActor ? DbActor.CreateChannelsFromList(List(
          (coinChannel, ObjectType.COIN),
          (laoChannel, ObjectType.LAO),
          (socialChannel, ObjectType.CHIRP),
          (reactionChannel, ObjectType.REACTION)
        ))
        // write lao creation message
        _ <- dbActor ? DbActor.WriteCreateLaoMessage(laoChannel, message)
        // write lao data
        _ <- dbActor ? DbActor.WriteLaoData(laoChannel, message, address)
        // after creating the lao, we need to send a lao#greet message to the frontend
        greet: GreetLao = GreetLao(data.id, message.sender, address.get, List.empty)
        _ <- dbBroadcast(rpcMessage, laoChannel, GreetLaoFormat.write(greet), laoChannel)
      } yield ()

    Await.ready(ask, duration).value.get match {
      case Success(_)                        => Right(rpcMessage)
      case Failure(ex: DbActorNAckException) => Left(PipelineError(ex.code, s"handleCreateLao failed : ${ex.message}", rpcMessage.getId))
      case reply                             => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleCreateLao failed : unexpected DbActor reply '$reply'", rpcMessage.getId))
    }
  }

  def handleGreetLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask = dbActor ? DbActor.ChannelExists(rpcMessage.getParamsChannel)
    Await.ready(ask, duration).value.get match {
      // We want to write greetLao only if channel exists
      case Success(_) =>
        val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
        Await.result(ask, duration)
      case Failure(_) => Left(PipelineError(ErrorCodes.INVALID_ACTION.id, s"handleGreetLao failed : Channel doesn't exist", rpcMessage.getId))
      case reply      => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleGreetLao failed : unexpected DbActor reply '$reply'", rpcMessage.getId))
    }
  }

  def handleStateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    val modificationId: Hash = rpcMessage.getDecodedData.asInstanceOf[StateLao].modification_id
    Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "NOT IMPLEMENTED : handleStateLao is not implemented", rpcMessage.id))
  }

  def handleUpdateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    // FIXME: the main channel is not updated
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration)
  }
}
