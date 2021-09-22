package ch.epfl.pop.pubsub.graph.handlers

import akka.NotUsed
import akka.pattern.AskableActorRef
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.graph.{DbActor, ErrorCodes, GraphMessage, PipelineError}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait MessageHandler extends AskPatternConstants {
  implicit final val dbActor: AskableActorRef = DbActor.getInstance

  val handler: Flow[GraphMessage, GraphMessage, NotUsed]

  def dbAskWritePropagate(rpcMessage: JsonRpcRequest): Future[GraphMessage] = {
    val message: Message = rpcMessage.getParamsMessage.get
    val ask: Future[GraphMessage] = (dbActor ? DbActor.WriteAndPropagate(rpcMessage.getParamsChannel, message)).map {
      case DbActor.DbActorWriteAck => Left(rpcMessage)
      case DbActor.DbActorNAck(code, description) => Right(PipelineError(code, description, rpcMessage.id))
      case _ => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, "Database actor returned an unknown answer", rpcMessage.id))
    }

    ask
  }
}
