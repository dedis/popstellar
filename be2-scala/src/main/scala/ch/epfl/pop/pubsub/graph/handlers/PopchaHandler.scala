package ch.epfl.pop.pubsub.graph.handlers

import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.method.message.data.popcha.Authenticate
import ch.epfl.pop.model.network.method.message.data.rollCall.OpenRollCall
import ch.epfl.pop.model.network.{JsonRpcMessage, JsonRpcRequest}
import ch.epfl.pop.model.objects.{Channel, DbActorNAckException, Hash, PublicKey}
import ch.epfl.pop.pubsub.graph.validators.MessageValidator.extractData
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor
import ch.epfl.pop.storage.DbActor.{DbActorAck, DbActorReadUserAuthenticationAck, ReadUserAuthenticated, WriteUserAuthenticated}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

/** Handler for Popcha related messages
  */
object PopchaHandler extends MessageHandler {
  final lazy val handlerInstance = new PopchaHandler(super.dbActor)

  def handleAuthentication(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleAuthentication(rpcMessage)
}

class PopchaHandler(dbRef: => AskableActorRef) extends MessageHandler {
  /** Handles authentication messages
   *
   * @param rpcMessage
   * message received
   * @return
   * a graph message representing the message's state after handling
   */
  def handleAuthentication(rpcMessage: JsonRpcRequest): GraphMessage = {
    val (authenticate, _, sender, _) = extractData[Authenticate](rpcMessage)
    for {
      result <- registerAuthentication(rpcMessage, sender, authenticate.clientId, authenticate.identifier)
    } yield result
  }

  private def registerAuthentication(rpcMessage: JsonRpcMessage, popToken: PublicKey, clientId: String, user: PublicKey): GraphMessage = {
    def invalidAuthError(duplicateUser: PublicKey) = PipelineError(ErrorCodes.INVALID_ACTION.id,
      s"handleAuthentication failed: $duplicateUser already registered for ($popToken,$clientId) pair (received $user instead)", rpcMessage.getId)

    def invalidDBResponseError(ex: DbActorNAckException) = PipelineError(ex.code,
      s"handleAuthentication failed : ${ex.message}", rpcMessage.getId)

    def invalidReply(reply: Any) = PipelineError(ErrorCodes.SERVER_ERROR.id,
      s"handleAuthentication failed : unexpected DbActor reply '$reply'", rpcMessage.getId)

    val askAuth = dbRef ? ReadUserAuthenticated(popToken, clientId)
    val alreadyRegistered = Await.ready(askAuth, duration).value.get match {
      case Success(DbActorReadUserAuthenticationAck(optUser)) => optUser match {
        case Some(userId) if userId == user => true
        case Some(userId) => return Left(invalidAuthError(userId))
        case None => false
      }
      case Failure(ex: DbActorNAckException) => return Left(invalidDBResponseError(ex))
      case reply => return Left(invalidReply(reply))
    }

    if (alreadyRegistered) {
      return Right(rpcMessage)
    }

    val askWrite = dbRef ? WriteUserAuthenticated(popToken, clientId, user)
    Await.ready(askWrite, duration).value.get match {
      case Success(Some(DbActorAck())) => Right(rpcMessage)
      case Failure(ex: DbActorNAckException) => Left(invalidDBResponseError(ex))
      case reply => Left(invalidReply(reply))
    }
  }

  private def generateOpenIdToken(authenticate: Authenticate): Unit = {
    // TODO
  }
}
