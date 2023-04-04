package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.pattern.AskableActorRef
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Partition}
import ch.epfl.pop.model.network.method.{Catchup, GetMessagesById, Heartbeat, Subscribe, Unsubscribe}
import ch.epfl.pop.model.network.{JsonRpcRequest, JsonRpcResponse}
import ch.epfl.pop.model.objects.{Channel, Hash}
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.pubsub.{AskPatternConstants, ClientActor, PubSubMediator}
import ch.epfl.pop.storage.DbActor
import ch.epfl.pop.model.network.MethodType
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import ch.epfl.pop.model.network.method.message.Message
import scala.collection.mutable
import akka.stream.FlowShape
import ch.epfl.pop.model.network.JsonRpcResponse
import scala.concurrent.{Await, Future}

object ParamsWithMapHandler extends AskPatternConstants {

  def graph(dbActorRef: AskableActorRef): Flow[GraphMessage, GraphMessage, NotUsed] = Flow.fromGraph(GraphDSL.create() {
    implicit builder: GraphDSL.Builder[NotUsed] =>
      {
        import GraphDSL.Implicits._

        /* partitioner port numbers */
        val portPipelineError = 0
        val portHeartBeatHandler = 1
        val portGetMsgsByIdHandler = 2
        val totalPorts = 3

        /* building blocks */
        val handlerPartitioner = builder.add(Partition[GraphMessage](
          totalPorts,
          {
            case Right(jsonRpcMessage: JsonRpcRequest) => jsonRpcMessage.getParams match {
                case _: Heartbeat       => portHeartBeatHandler
                case _: GetMessagesById => portGetMsgsByIdHandler

              }
            case _ => portPipelineError // Pipeline error goes directly in handlerMerger
          }
        ))

        val heartbeatHandler = builder.add(ParamsWithMapHandler.heartBeatHandler(dbActorRef))
        val getMessagesByIdHandler = builder.add(ParamsWithMapHandler.getMessagesByIdHandler(dbActorRef))

        val handlerMerger = builder.add(Merge[GraphMessage](totalPorts))

        /* glue the components together */
        handlerPartitioner.out(portPipelineError) ~> handlerMerger
        handlerPartitioner.out(portHeartBeatHandler) ~> heartBeatHandler ~> handlerMerger
        handlerPartitioner.out(portGetMsgsByIdHandler) ~> getMessagesByIdHandler ~> handlerMerger

        /* close the shape */
        FlowShape(handlerPartitioner.in, handlerMerger.out)


      }
  })

  def heartBeatHandler(dbActorRef: AskableActorRef): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map({
    case Right(jsonRpcMessage: JsonRpcRequest) =>
      val receivedHeartBeat: Map[Channel, Set[Hash]] = jsonRpcMessage.getParams.asInstanceOf[Heartbeat].channelsToMessageIds // pas sûr
      val ask = dbActorRef ? DbActor.GetSetOfChannels()
      val answer = Await.result(ask, duration)
      val setOfChannels: Set[Channel] = answer.asInstanceOf[DbActor.DbActorGetSetOfChannelsAck].channels.map(s => Channel(s))
      var localHeartBeat: mutable.HashMap[Channel, List[Hash]] = mutable.HashMap()
      setOfChannels.foreach(channel => {
        val ask = dbActorRef ? DbActor.ReadChannelData(channel)
        val answer = Await.result(ask, duration)
        val setOfIds = answer.asInstanceOf[DbActor.DbActorReadChannelDataAck].channelData.messages.to(collection.mutable.Set)
        localHeartBeat += (channel -> setOfIds)
      })
      var missingIds: mutable.HashMap[Channel, Set[Hash]] = mutable.HashMap()
      receivedHeartBeat.keys.foreach(channel => {
        if (localHeartBeat.contains(channel)) {
          missingIds += (channel -> receivedHeartBeat.get(channel).get.filter(id =>
            !localHeartBeat.get(channel).contains(id)
          ))
        }
      })
      Right(JsonRpcRequest(RpcValidator.JSON_RPC_VERSION, MethodType.HEARTBEAT, Heartbeat(missingIds.toMap), None)) // how to pass the missing ids?? répondre par un getmsgsbyid et gérer les ids.

    case Right(jsonRpcMessage: JsonRpcResponse) =>
      Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "HeartbeatHandler received a 'JsonRpcResponse'", jsonRpcMessage.id))
    case graphMessage @ _ => graphMessage
  })

  def getMessagesByIdHandler(dbActorRef: AskableActorRef): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map({
    case Right(jsonRpcMessage: JsonRpcRequest) => {
      val receivedRequest: Map[Channel, Set[Hash]] = jsonRpcMessage.getParams.asInstanceOf[GetMessagesById].channelsToMessageIds
      val ask = dbActorRef ? DbActor.GetSetOfChannels()
      val answer = Await.result(ask, duration)
      val setOfChannels: Set[Channel] = answer.asInstanceOf[DbActor.DbActorGetSetOfChannelsAck].channels.map(s => Channel(s))
      var response: mutable.HashMap[Channel, Set[Message]] = mutable.HashMap()
      receivedRequest.keys.foreach(channel => {
        val ask = dbActorRef ? DbActor.Catchup(channel)
        val answer = Await.result(ask, duration)
        val missingIds = answer.asInstanceOf[DbActor.DbActorCatchupAck].messages.filter(message => receivedRequest.get(channel).contains(message.message_id)).to(collection.mutable.Set) // je retrieve tous les messages pour ne garder que ceux qui m'intéresse :((
        response += (channel -> missingIds)
      })
      Right(JsonRpcResponse(RpcValidator.JSON_RPC_VERSION, Some(response), None, None))

    }
  })

}
