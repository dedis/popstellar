package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.pattern.AskableActorRef
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.{GetMessagesById, Heartbeat}
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse, MethodType, ResultObject}
import ch.epfl.pop.model.objects.{Channel, DbActorNAckException, Hash}
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor

import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.concurrent.Await
import scala.util.{Failure, Success}

object ParamsWithMapHandler extends AskPatternConstants {

  def heartbeatHandler(dbActorRef: AskableActorRef): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Right(jsonRpcMessage: JsonRpcRequest) =>
      /** first step is to retrieve the received heartbeat from the jsonRpcRequest */
      val receivedHeartBeat: Map[Channel, Set[Hash]] = jsonRpcMessage.getParams.asInstanceOf[Heartbeat].channelsToMessageIds

      /** second step is to retrieve the local set of channels */
      var setOfChannels: Set[Channel] = Set()
      val ask = dbActorRef ? DbActor.GetAllChannels()
      Await.ready(ask, duration).value match {
        case Some(Success(DbActor.DbActorGetAllChannelsAck(channels))) =>
          setOfChannels = channels
        case Some(Failure(ex: DbActorNAckException)) =>
          Left(PipelineError(ex.code, s"couldn't retrieve local set of channels", jsonRpcMessage.getId))
        case reply =>
          Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"heartbeatHandler failed : unexpected DbActor reply '$reply'", jsonRpcMessage.getId))
      }

      /** third step is to ask the DB for the content of each channel in terms of message ids. */
      val localHeartBeat: mutable.HashMap[Channel, Set[Hash]] = mutable.HashMap()
      setOfChannels.foreach(channel => {
        val ask = dbActorRef ? DbActor.ReadChannelData(channel)
        Await.ready(ask, duration).value match {
          case Some(Success(DbActor.DbActorReadChannelDataAck(channelData))) =>
            val setOfIds = channelData.messages.toSet
            localHeartBeat += (channel -> setOfIds)
          case Some(Failure(ex: DbActorNAckException)) =>
            Left(PipelineError(ex.code, s"couldn't readChannelData for local heartbeat", jsonRpcMessage.getId))
          case reply =>
            Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"heartbeatHandler failed : unexpected DbActor reply '$reply'", jsonRpcMessage.getId))
        }
      })

      /** finally, we only keep from the received heartbeat the message ids that are not contained in the locally extracted heartbeat. */
      var missingIdsMap: HashMap[Channel, Set[Hash]] = HashMap()
      receivedHeartBeat.keys.foreach(channel => {
        val missingIdsSet = receivedHeartBeat(channel).diff(localHeartBeat.getOrElse(channel, Set.empty))
        if (missingIdsSet.nonEmpty)
          missingIdsMap += (channel -> missingIdsSet)
      })
      Right(JsonRpcRequest(RpcValidator.JSON_RPC_VERSION, MethodType.GET_MESSAGES_BY_ID, GetMessagesById(missingIdsMap), Some(0)))

    case Right(jsonRpcMessage: JsonRpcResponse) =>
      Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "HeartbeatHandler received a 'JsonRpcResponse'", jsonRpcMessage.id))
    case graphMessage @ _ => graphMessage
  }.filter(!isGetMessagesByIdEmpty(_)) // Answer to heartbeats only if some messages are actually missing

  def getMessagesByIdHandler(dbActorRef: AskableActorRef): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Right(jsonRpcMessage: JsonRpcRequest) =>
      val receivedRequest: Map[Channel, Set[Hash]] = jsonRpcMessage.getParams.asInstanceOf[GetMessagesById].channelsToMessageIds
      val response: mutable.HashMap[Channel, Set[Message]] = mutable.HashMap()
      receivedRequest.keys.foreach(channel => {
        val ask = dbActorRef ? DbActor.Catchup(channel)
        Await.ready(ask, duration).value match {
          case Some(Success(DbActor.DbActorCatchupAck(messages))) =>
            val missingMessages = messages.filter(message => receivedRequest(channel).contains(message.message_id)).toSet
            if (missingMessages.nonEmpty)
              response += (channel -> missingMessages)
          case Some(Failure(ex: DbActorNAckException)) =>
            Left(PipelineError(ex.code, s"getMessagesByIdHandler failed : ${ex.message}", jsonRpcMessage.getId))
          case reply =>
            Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"AnswerGenerator failed : unexpected DbActor reply '$reply'", jsonRpcMessage.getId))
        }
      })
      Right(JsonRpcResponse(RpcValidator.JSON_RPC_VERSION, new ResultObject(HashMap.from(response)), jsonRpcMessage.id))

    case Right(jsonRpcMessage: JsonRpcResponse) =>
      Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "getMessagesByIdHandler received a 'JsonRpcResponse'", jsonRpcMessage.id))
    case graphMessage @ _ => graphMessage
  }

  private def isGetMessagesByIdEmpty(graphMessage: GraphMessage): Boolean = {
    graphMessage match {
      case Right(value) =>
        value match {
          case (response: JsonRpcRequest) => response
              .getParams
              .asInstanceOf[GetMessagesById]
              .channelsToMessageIds
              .isEmpty
        }
      case _ => false
    }
  }

}
