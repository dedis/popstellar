package ch.epfl.pop.pubsub.graph

import akka.NotUsed
import akka.pattern.AskableActorRef
import akka.stream.scaladsl.Flow
import ch.epfl.pop.model.network.method.{Broadcast, Catchup}
import ch.epfl.pop.model.network.{ResultObject, _}
import ch.epfl.pop.model.objects.DbActorNAckException
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import ch.epfl.pop.storage.DbActor

import scala.concurrent.Await
import scala.util.{Failure, Success}

/**
 * Object for AnswerGenerator to keep a compatible interface
 * Since this is an object only one instance of AnswerGenerator(class) will be created
 *
 */
object AnswerGenerator extends AskPatternConstants {
  lazy val dbActor: AskableActorRef = DbActor.getInstance
  val answerGen = new AnswerGenerator(dbActor)

  def generateAnswer(graphMessage: GraphMessage): GraphMessage = answerGen.generateAnswer(graphMessage)

  val generator: Flow[GraphMessage, GraphMessage, NotUsed] = answerGen.generator
}

class AnswerGenerator(dbActor: => AskableActorRef) extends AskPatternConstants {

  def generateAnswer(graphMessage: GraphMessage): GraphMessage = graphMessage match {
    // Note: the output message (if successful) is an answer
    // The standard output is always a JsonMessage (pipeline errors are transformed into negative answers)

    case Left(rpcRequest: JsonRpcRequest) => rpcRequest.getParams match {
      case Catchup(channel) =>
        val askCatchup = dbActor ? DbActor.Catchup(channel)
        Await.ready(askCatchup, duration).value match {
          case Some(Success(DbActor.DbActorCatchupAck(messages))) =>
            val resultObject: ResultObject = new ResultObject(messages)
            Left(JsonRpcResponse(RpcValidator.JSON_RPC_VERSION, Some(resultObject), None, rpcRequest.id))
          case Some(Failure(ex: DbActorNAckException)) => Right(PipelineError(ex.code, s"AnswerGenerator failed : ${ex.message}", rpcRequest.getId))
          case reply => Right(PipelineError(ErrorCodes.SERVER_ERROR.id, s"AnswerGenerator failed : unexpected DbActor reply '$reply'", rpcRequest.getId))
        }


      // Note: this is not going to remain true when server-to-server communication gets implemented
      case Broadcast(_, _) => Right(PipelineError(
        ErrorCodes.SERVER_ERROR.id,
        "Server received a Broadcast message which should never happen (broadcast messages are only emitted by server)",
        rpcRequest.id
      ))

      // Standard answer res == 0
      case _ => Left(JsonRpcResponse(
        RpcValidator.JSON_RPC_VERSION, Some(new ResultObject(0)), None, rpcRequest.id
      ))
    }

    // Convert PipelineErrors into negative JsonRpcResponses
    case Right(pipelineError: PipelineError) => Left(JsonRpcResponse(
      RpcValidator.JSON_RPC_VERSION,
      None,
      Some(ErrorObject(pipelineError.code, pipelineError.description)),
      pipelineError.rpcId
    ))

    // /!\ If something is outputted as Right(...), then there's a mistake somewhere in the graph!
    case _ => Right(PipelineError(
      ErrorCodes.SERVER_ERROR.id,
      s"Internal server error: unknown reason. The MessageEncoder could not decide what to do with input $graphMessage",
      None
    ))
  }

  val generator: Flow[GraphMessage, GraphMessage, NotUsed] = Flow[GraphMessage].map(generateAnswer)
}
