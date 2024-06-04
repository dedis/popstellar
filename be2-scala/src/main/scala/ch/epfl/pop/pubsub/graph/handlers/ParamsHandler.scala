package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.AskableActorRef
import akka.stream.scaladsl.Flow
import ch.epfl.pop.decentralized.ConnectionMediator
import ch.epfl.pop.model.network.{ErrorObject, JsonRpcRequest, JsonRpcResponse, MethodType, ResultObject, ResultRumor}
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.{GreetServer, Rumor, RumorState}
import ch.epfl.pop.model.objects.{Channel, PublicKey}
import ch.epfl.pop.pubsub.ClientActor.ClientAnswer
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.pubsub.{AskPatternConstants, ClientActor, MessageRegistry, PubSubMediator}
import ch.epfl.pop.storage.DbActor
import ch.epfl.pop.storage.DbActor.{DbActorAck, DbActorGenerateRumorStateAns, DbActorGetRumorStateAck, DbActorReadRumor, GetRumorState, ReadRumor, WriteRumor}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

object ParamsHandler extends AskPatternConstants {

  def subscribeHandler(clientActorRef: AskableActorRef): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Right(jsonRpcMessage: JsonRpcRequest) =>
      val channel: Channel = jsonRpcMessage.getParams.channel
      val ask: Future[GraphMessage] = (clientActorRef ? ClientActor.SubscribeTo(jsonRpcMessage.getParams.channel)).map {
        case PubSubMediator.SubscribeToAck(returnedChannel) if returnedChannel == channel =>
          Right(jsonRpcMessage)
        case PubSubMediator.SubscribeToAck(returnedChannel) =>
          Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"PubSubMediator subscribed client to channel '$returnedChannel' instead of '$channel'", jsonRpcMessage.id))
        case PubSubMediator.SubscribeToNAck(returnedChannel, reason) if returnedChannel == channel =>
          Left(PipelineError(ErrorCodes.INVALID_ACTION.id, s"Could not subscribe client to channel '$returnedChannel': $reason", jsonRpcMessage.id))
        case PubSubMediator.SubscribeToNAck(returnedChannel, reason) => Left(PipelineError(
            ErrorCodes.SERVER_ERROR.id,
            s"PubSubMediator tried to subscribe client to channel '$returnedChannel' instead of '$channel' but could not: $reason",
            jsonRpcMessage.id
          ))
        case _ =>
          Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "Client actor returned an unknown answer", jsonRpcMessage.id))
      }

      Await.result(ask, duration)

    case Right(jsonRpcMessage: JsonRpcResponse) =>
      Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "SubscribeHandler received a 'JsonRpcResponse'", jsonRpcMessage.id))
    case graphMessage @ _ => graphMessage
  }

  def unsubscribeHandler(clientActorRef: AskableActorRef): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Right(jsonRpcMessage: JsonRpcRequest) =>
      val channel: Channel = jsonRpcMessage.getParams.channel
      val ask: Future[GraphMessage] = (clientActorRef ? ClientActor.UnsubscribeFrom(channel)).map {
        case PubSubMediator.UnsubscribeFromAck(returnedChannel) if returnedChannel == channel =>
          Right(jsonRpcMessage)
        case PubSubMediator.UnsubscribeFromAck(returnedChannel) =>
          Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"PubSubMediator unsubscribe client from channel '$returnedChannel' instead of '$channel'", jsonRpcMessage.id))
        case PubSubMediator.UnsubscribeFromNAck(returnedChannel, reason) if returnedChannel == channel =>
          Left(PipelineError(ErrorCodes.INVALID_ACTION.id, s"Could not unsubscribe client from channel '$returnedChannel': $reason", jsonRpcMessage.id))
        case PubSubMediator.UnsubscribeFromNAck(returnedChannel, reason) => Left(PipelineError(
            ErrorCodes.SERVER_ERROR.id,
            s"PubSubMediator tried to unsubscribe client from channel '$returnedChannel' instead of '$channel' but could not: $reason",
            jsonRpcMessage.id
          ))
        case _ =>
          Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "Client actor returned an unknown answer", jsonRpcMessage.id))
      }

      Await.result(ask, duration)

    case Right(jsonRpcMessage: JsonRpcResponse) =>
      Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "UnsubscribeHandler received a 'JsonRpcResponse'", jsonRpcMessage.id))
    case graphMessage @ _ => graphMessage
  }

  // Catchup requests are treated at the AnswerGenerator stage since it generates a JsonRpcResponse directly
  def catchupHandler(clientActorRef: AskableActorRef): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map(m => m)

  def greetServerHandler(clientActorRef: ActorRef): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Right(jsonRpcMessage: JsonRpcRequest) =>
      jsonRpcMessage.method match {
        case MethodType.greet_server =>
          val greetServer: GreetServer = jsonRpcMessage.getParams.asInstanceOf[GreetServer]
          clientActorRef ! greetServer
          Right(jsonRpcMessage)
        case _ => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "GreetServerHandler received a non expected jsonRpcRequest", jsonRpcMessage.id))
      }
    case Right(jsonRpcMessage: JsonRpcResponse) =>
      Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "GreetServerHandler received a 'JsonRpcResponse'", jsonRpcMessage.id))
    case graphMessage @ _ => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "GreetServerHandler received an unexpected message:" + graphMessage, None))
  }.filter(_ => false)

  def rumorHandler(dbActorRef: AskableActorRef, messageRegistry: MessageRegistry)(implicit system: ActorSystem): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Right(jsonRpcMessage: JsonRpcRequest) =>
      jsonRpcMessage.method match {
        case MethodType.rumor =>
          val rumor: Rumor = jsonRpcMessage.getParams.asInstanceOf[Rumor]
          // check if rumor already received
          val readRumorStateDb = dbActorRef ? GetRumorState()
          Await.result(readRumorStateDb, duration) match {
            case DbActorGetRumorStateAck(rumorState) =>
              // expected
              rumorState.state.get(rumor.senderPk) match
                case Some(rumorIdDb) if rumorIdDb + 1 == rumor.rumorId =>
                  tryProcessExpectedRumor(jsonRpcMessage, dbActorRef, messageRegistry)
                case None if rumor.rumorId == 0 =>
                  tryProcessExpectedRumor(jsonRpcMessage, dbActorRef, messageRegistry)
                // not Expected
                case _ =>
                  Left(PipelineError(ErrorCodes.ALREADY_EXISTS.id, s"Rumor ${rumor.rumorId} with jsonRpcId : ${jsonRpcMessage.id} already exists", jsonRpcMessage.id))
          }
        case _ => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "RumorHandler received a non expected jsonRpcRequest", jsonRpcMessage.id))
      }
    case graphMessage @ _ => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "RumorHandler received an unexpected message:" + graphMessage, None))
  }

  private def tryProcessExpectedRumor(jsonRpcMessage: JsonRpcRequest, dbActorRef: AskableActorRef, messageRegistry: MessageRegistry)(implicit system: ActorSystem): GraphMessage = {
    val rumor: Rumor = jsonRpcMessage.getParams.asInstanceOf[Rumor]
    if (ProcessMessagesHandler.rumorHandler(messageRegistry, rumor)) {
      val dbWrite = dbActorRef ? WriteRumor(rumor)
      Await.result(dbWrite, duration) match
        case DbActorAck() =>
          system.log.info(s"All messages from rumor ${rumor.rumorId} were processed correctly")
          return Right(jsonRpcMessage)
    }
    system.log.info(s"Some messages from rumor ${rumor.rumorId} were not processed")
    Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"Some messages from Rumor ${rumor.rumorId} with jsonRpcId : ${jsonRpcMessage.id} couldn't be processed", jsonRpcMessage.id))
  }

  def rumorStateHandler(dbActorRef: AskableActorRef): Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map {
    case Right(jsonRpcRequest: JsonRpcRequest) =>
      jsonRpcRequest.method match
        case MethodType.rumor_state =>
          val rumorState = jsonRpcRequest.getParams.asInstanceOf[RumorState]
          val generateRumorStateAns = dbActorRef ? DbActor.GenerateRumorStateAns(rumorState)
          Await.result(generateRumorStateAns, duration) match
            case DbActorGenerateRumorStateAns(rumorList) =>
              Right(JsonRpcResponse(RpcValidator.JSON_RPC_VERSION, new ResultObject(ResultRumor(rumorList)), jsonRpcRequest.id))
            case _ => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"RumorStateHandler was not able to generate rumor state answer", jsonRpcRequest.id))
        case graphMessage @ _ => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"RumorStateHandler received a message with unexpected method :$graphMessage", jsonRpcRequest.id))
    case graphMessage @ _ => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"RumorStateHandler received an unexpected message:$graphMessage", None))

  }

}
