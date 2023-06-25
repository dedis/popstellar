package ch.epfl.pop.pubsub.graph.handlers

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import akka.http.scaladsl.model.{StatusCodes, Uri}
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

  def handleAuthentication(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleAuthentication(rpcMessage, None)
}

class PopchaHandler(dbRef: => AskableActorRef) extends MessageHandler {

  private val TOKEN_VALIDITY_DURATION = 3600

  /** Handles authentication messages
    *
    * @param rpcMessage
    *   message received
    * @return
    *   a graph message representing the message's state after handling
    */
  def handleAuthentication(rpcMessage: JsonRpcRequest, responseHandler: Option[Uri => GraphMessage]): GraphMessage = {
    val (authenticate, laoId, sender, _) = extractData[Authenticate](rpcMessage)

    val respHandler: Uri => GraphMessage = responseHandler match {
      case Some(handler) => handler // dependency injection case
      case None          => sendResponseToWebsocket(rpcMessage, authenticate, laoId) // default case
    }

    for {
      _ <- registerAuthentication(rpcMessage, sender, authenticate.clientId, authenticate.identifier)
      result <- sendOpenIdToken(rpcMessage, authenticate, laoId, respHandler)
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
      case Success(DbActorAck)               => Right(rpcMessage)
      case Failure(ex: DbActorNAckException) => Left(invalidDBResponseError(ex))
      case reply                             => Left(invalidReply(reply))
    }
  }

  private def generateOpenIdToken(rpcMessage: JsonRpcMessage, authenticate: Authenticate, laoId: Hash): Either[PipelineError, String] = {
    val date = Calendar.getInstance()
    val issuedTime = date.getTime
    val expTime = new Date(date.getTimeInMillis + (TOKEN_VALIDITY_DURATION * 1000)) // Expire after 1h

    // TODO: public key registered in the fake client --> when supporting popcha server discovery, a secured server-specific private key should be used instead
    val publicKey = "ThisIsADummyKeyPleaseUpdateItBeforeUsageMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsbR9Ip84tR4vc1IEefBJ\\ndHMlQAQm1UltYE3vs875eY8ASZ4lzlLG6iVRe7LH4VN6j7GB4Tjj2EtgUFUAQqbF\\ns5mn7cFO7DR9riQDgLGekAQ5g/mLz9QhuAGjU2am0mPBOSBME08Ek9vNRfAOGVWk\\n9fDUhdRceRdKOXnnz+YvqYfe3vz4jx9XXJHZHmG2wNB6egCnsbZOuEqiWVMj5+3w\\nKt1prUGKEAHtPqC+olDaLwZw1didYotPgaZDwedkcAVSWNHvOkkY3uMqvKI+Cpox\\nP+uqtdy9tM54sNQjoWdq4LIaWF/nLRy5fM2JAVbAqwPW6z23YMi4HIsfwuj+d8UZ\\ntQIDAQAB"

    val algorithm = Algorithm.HMAC256(publicKey)

    Try(
      JWT.create()
        .withIssuer(RuntimeEnvironment.ownServerAddress)
        .withSubject(authenticate.identifier.base64Data.toString)
        .withAudience(authenticate.clientId)
        .withIssuedAt(issuedTime)
        .withExpiresAt(expTime)
        .withClaim("nonce", authenticate.nonce.toString)
        .withClaim("laoId", laoId.base64Data.toString)
        .withClaim("auth_time", issuedTime)
        .sign(algorithm)
    ) match {
      case Success(jwt) => Right(jwt)
      case Failure(ex)  => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleAuthentication failed : ${ex.getMessage}", rpcMessage.getId))
    }
  }

  private def sendOpenIdToken(rpcMessage: JsonRpcMessage, authenticate: Authenticate, laoId: Hash, responseFlowHandler: Uri => GraphMessage): GraphMessage = {
    val jwt = generateOpenIdToken(rpcMessage, authenticate, laoId) match {
      case Right(token) => token
      case Left(error)  => return Left(error)
    }

    val messageToSend = Uri("").withQuery(Query(Map(
      "token_type" -> "bearer",
      "id_token" -> jwt,
      "expires_in" -> TOKEN_VALIDITY_DURATION.toString,
      "state" -> authenticate.state
    )))

    responseFlowHandler(messageToSend)
  }

  private def sendResponseToWebsocket(rpcMessage: JsonRpcMessage, authenticate: Authenticate, laoId: Hash)(response: Uri): GraphMessage = {
    implicit val subSystem: ActorSystem = ActorSystem()
    import subSystem.dispatcher

    val wsAddress = s"ws://${authenticate.popchaAddress}/response/$laoId/authentication/${authenticate.clientId}/${authenticate.nonce}"

    val tokenEmissionSource: Source[Message, NotUsed] = Source.single(TextMessage(response.toString()))
    val responseFlow: Flow[Message, Message, NotUsed] = Flow.fromSinkAndSourceCoupled(Sink.ignore, tokenEmissionSource)

    val (upgradeResponse, _) =
      Http().singleWebSocketRequest(WebSocketRequest(wsAddress), responseFlow)

    val connected = upgradeResponse.map { upgrade =>
      if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
        Done
      } else {
        return Left(PipelineError(ErrorCodes.SERVER_ERROR.id, "handleAuthentication failed : failed to upgrade web socket request", rpcMessage.getId))
      }
    }

    Await.ready(connected, duration).value.get match {
      case Success(_)  => Right(rpcMessage)
      case Failure(ex) => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleAuthentication failed : ${ex.getMessage}", rpcMessage.getId))
    }
  }
}
