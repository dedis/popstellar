package ch.epfl.pop.pubsub.graph.handlers

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import akka.pattern.AskableActorRef
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.{Done, NotUsed}
import ch.epfl.pop.config.RuntimeEnvironment
import ch.epfl.pop.model.network.method.message.data.popcha.Authenticate
import ch.epfl.pop.model.network.{JsonRpcMessage, JsonRpcRequest}
import ch.epfl.pop.model.objects.{DbActorNAckException, Hash, PublicKey}
import ch.epfl.pop.pubsub.graph.validators.MessageValidator.extractData
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor.{DbActorAck, DbActorReadUserAuthenticationAck, ReadUserAuthenticated, WriteUserAuthenticated}
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm

import java.util.{Calendar, Date}
import scala.concurrent.Await
import scala.util.{Failure, Success, Try}

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
    *   message received
    * @return
    *   a graph message representing the message's state after handling
    */
  def handleAuthentication(rpcMessage: JsonRpcRequest): GraphMessage = {
    val (authenticate, laoId, sender, _) = extractData[Authenticate](rpcMessage)
    for {
      _ <- registerAuthentication(rpcMessage, sender, authenticate.clientId, authenticate.identifier)
      result <- generateAndSendOpenIdToken(rpcMessage, authenticate, laoId)
    } yield result
  }

  private def registerAuthentication(rpcMessage: JsonRpcMessage, popToken: PublicKey, clientId: String, user: PublicKey): GraphMessage = {
    def invalidAuthError(duplicateUser: PublicKey) = PipelineError(
      ErrorCodes.INVALID_ACTION.id,
      s"handleAuthentication failed: $duplicateUser already registered for ($popToken,$clientId) pair (received $user instead)",
      rpcMessage.getId
    )

    def invalidDBResponseError(ex: DbActorNAckException) = PipelineError(ex.code, s"handleAuthentication failed : ${ex.message}", rpcMessage.getId)

    def invalidReply(reply: Any) = PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleAuthentication failed : unexpected DbActor reply '$reply'", rpcMessage.getId)

    val askAuth = dbRef ? ReadUserAuthenticated(popToken, clientId)
    val alreadyRegistered = Await.ready(askAuth, duration).value.get match {
      case Success(DbActorReadUserAuthenticationAck(optUser)) => optUser match {
          case Some(userId) if userId == user => true
          case Some(userId)                   => return Left(invalidAuthError(userId))
          case None                           => false
        }
      case Failure(ex: DbActorNAckException) => return Left(invalidDBResponseError(ex))
      case reply                             => return Left(invalidReply(reply))
    }

    if (alreadyRegistered) {
      return Right(rpcMessage)
    }

    val askWrite = dbRef ? WriteUserAuthenticated(popToken, clientId, user)
    Await.ready(askWrite, duration).value.get match {
      case Success(DbActorAck())             => Right(rpcMessage)
      case Failure(ex: DbActorNAckException) => Left(invalidDBResponseError(ex))
      case reply                             => Left(invalidReply(reply))
    }
  }

  private def generateAndSendOpenIdToken(rpcMessage: JsonRpcMessage, authenticate: Authenticate, laoId: Hash): GraphMessage = {
    val date = Calendar.getInstance()
    val issuedTime = date.getTime
    val expTime = new Date(date.getTimeInMillis + (60 * 60 * 1000)) // Expire after 1h

    val publicKey = "ThisIsADummyKeyPleaseUpdateItBeforeUsageMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsbR9Ip84tR4vc1IEefBJ\\ndHMlQAQm1UltYE3vs875eY8ASZ4lzlLG6iVRe7LH4VN6j7GB4Tjj2EtgUFUAQqbF\\ns5mn7cFO7DR9riQDgLGekAQ5g/mLz9QhuAGjU2am0mPBOSBME08Ek9vNRfAOGVWk\\n9fDUhdRceRdKOXnnz+YvqYfe3vz4jx9XXJHZHmG2wNB6egCnsbZOuEqiWVMj5+3w\\nKt1prUGKEAHtPqC+olDaLwZw1didYotPgaZDwedkcAVSWNHvOkkY3uMqvKI+Cpox\\nP+uqtdy9tM54sNQjoWdq4LIaWF/nLRy5fM2JAVbAqwPW6z23YMi4HIsfwuj+d8UZ\\ntQIDAQAB"
    val algorithm = Algorithm.HMAC256(publicKey)

    val jwt = Try(
      JWT.create()
        .withIssuer(RuntimeEnvironment.ownAuthAddress)
        .withSubject(authenticate.identifier.base64Data.toString)
        .withAudience(authenticate.clientId)
        .withIssuedAt(issuedTime)
        .withExpiresAt(expTime)
        .withClaim("nonce", authenticate.nonce)
        .sign(algorithm)
    ) match {
      case Success(jwt)       => jwt
      case Failure(exception) => return Left(PipelineError(ErrorCodes.SERVER_ERROR.id, exception.getMessage, rpcMessage.getId))
    }

    sendOpenIdToken(rpcMessage, authenticate, laoId, jwt)
  }

  private def sendOpenIdToken(rpcMessage: JsonRpcMessage, authenticate: Authenticate, laoId: Hash, openIdToken: String): GraphMessage = {
    implicit val subSystem: ActorSystem = ActorSystem()
    import subSystem.dispatcher

    val tokenEmissionSource: Source[Message, NotUsed] = Source.single(TextMessage(openIdToken))
    val flow: Flow[Message, Message, NotUsed] = Flow.fromSinkAndSource(Sink.ignore, tokenEmissionSource)

    val wsAddress = s"ws://${authenticate.popchaAddress}/response/$laoId/${authenticate.clientId}/${authenticate.nonce}"
    val (upgradeResponse, _) =
      Http().singleWebSocketRequest(WebSocketRequest(wsAddress), flow)

    val connected = upgradeResponse.map { upgrade =>
      if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
        Done
      } else {
        return Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "Failed to upgrade web socket request", rpcMessage.getId))
      }
    }

    Right(rpcMessage)
  }
}
