package ch.epfl.pop.pubsub.graph.handlers

import ch.epfl.pop.config.RuntimeEnvironment.appConf
import ch.epfl.pop.config.ServerConf
import ch.epfl.pop.json.MessageDataProtocol.GreetLaoFormat
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.lao.{CreateLao, GreetLao, StateLao}
import ch.epfl.pop.model.objects.{Base64Data, Channel, DbActorNAckException, Hash}
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

case object LaoHandler extends MessageHandler {

  def handleCreateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    val params: Option[Message] = rpcMessage.getParamsMessage
    params match {
      case Some(message: Message) =>
        val data: CreateLao = message.decodedData.get.asInstanceOf[CreateLao]

        // we are using the lao id instead of the message_id at lao creation
        val laoChannel: Channel = Channel(s"${Channel.ROOT_CHANNEL_PREFIX}${data.id}")
        val coinChannel: Channel = Channel(s"$laoChannel${Channel.COIN_CHANNEL_PREFIX}")
        val socialChannel: Channel = Channel(s"$laoChannel${Channel.SOCIAL_MEDIA_CHIRPS_PREFIX}")
        val reactionChannel: Channel = Channel(s"$laoChannel${Channel.REACTIONS_CHANNEL_PREFIX}")
        // we get access to the canonical address of the server
        val config = ServerConf(appConf)
        val address: Option[String] = Some(f"ws://${config.interface}:${config.port}/${config.path}")

        val combined = for {
          // check whether the lao already exists in db
          _ <- {
            (dbActor ? DbActor.ChannelExists(laoChannel)).transformWith {
              case Success(_) => Future { throw DbActorNAckException(ErrorCodes.INVALID_ACTION.id, "lao already exists in db") }
              case _          => Future { () }
            }
          }
          // create lao channels
          _ <- dbActor ? DbActor.CreateChannelsFromList(List(
            (coinChannel, ObjectType.COIN),
            (laoChannel, ObjectType.LAO),
            (socialChannel, ObjectType.CHIRP),
            (reactionChannel, ObjectType.REACTION)
          ))
          // write lao creation message
          _ <- dbActor ? DbActor.Write(laoChannel, message)
          // write lao data
          _ <- dbActor ? DbActor.WriteLaoData(laoChannel, message, address)
          // after creating the lao, we need to send a lao#greet message to the frontend
          greet: GreetLao = GreetLao(data.id, params.get.sender, address.get, List.empty)
          _ <- dbBroadcast(rpcMessage, laoChannel, GreetLaoFormat.write(greet).toString(), laoChannel)
        } yield ()

        Await.ready(combined, duration).value.get match {
          case Success(_)                        => Left(rpcMessage)
          case Failure(ex: DbActorNAckException) => Right(PipelineError(ex.code, s"handleCreateLao failed : ${ex.message}", rpcMessage.getId))
          case reply                             => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleCreateLao failed : unexpected DbActor reply '$reply'", rpcMessage.getId))
        }

      case _ => Right(PipelineError(
          ErrorCodes.SERVER_ERROR.id,
          s"Unable to handle lao message $rpcMessage. Not a Publish/Broadcast message",
          rpcMessage.id
        ))
    }
  }

  def handleGreetLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration)
  }

  def handleStateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    val modificationId: Hash = rpcMessage.getDecodedData.asInstanceOf[StateLao].modification_id
    Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "NOT IMPLEMENTED : handleStateLao is not implemented", rpcMessage.id))
  }

  def handleUpdateLao(rpcMessage: JsonRpcRequest): GraphMessage = {
    // FIXME: the main channel is not updated
    val ask: Future[GraphMessage] = dbAskWritePropagate(rpcMessage)
    Await.result(ask, duration)
  }
}
