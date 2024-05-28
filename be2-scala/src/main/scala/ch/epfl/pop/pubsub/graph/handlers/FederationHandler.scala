package ch.epfl.pop.pubsub.graph.handlers

import akka.pattern.AskableActorRef
import ch.epfl.pop.json.MessageDataProtocol.*
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.federation.{FederationChallenge, FederationExpect, FederationInit, FederationResult}
import ch.epfl.pop.model.objects.*
import ch.epfl.pop.pubsub.PubSubMediator
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor
import ch.epfl.pop.storage.DbActor.{DbActorReadServerPrivateKeyAck, DbActorReadServerPublicKeyAck}
import spray.json.*

import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

object FederationHandler extends MessageHandler {
  final lazy val handlerInstance = new FederationHandler(super.dbActor, super.mediator)

  def handleFederationChallenge(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleFederationChallenge(rpcMessage)

  def handleFederationRequestChallenge(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleFederationChallengeRequest(rpcMessage)

  def handleFederationInit(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleFederationInit(rpcMessage)

  def handleFederationExpect(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleFederationExpect(rpcMessage)

  def handleFederationResult(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleFederationResult(rpcMessage)

}

class FederationHandler(dbRef: => AskableActorRef, mediatorRef: => AskableActorRef) extends MessageHandler {

  // TODO: CHANGE THE DESCRIPTION OF THE ERROR IN THE PIPELINE FOR EACH CASE TO BE EASY TO DEBUG
  override final val dbActor: AskableActorRef = dbRef
  override final val mediator: AskableActorRef = mediatorRef
  private final val CHALLENGE_NB_BYTES: Int = 32
  private val serverUnexpectedAnswer: String = "The server is doing something unexpected"
  private final val status: (String, String) = ("success", "failure")
  private final val reason: String = "Incorrect challenge"
  private final val keys: (String, String, String) = ("expect", "init", "challenge")

  def handleFederationChallenge(rpcMessage: JsonRpcRequest): GraphMessage = {
    var federationResult: FederationResult = null

    val ask = dbActor ? DbActor.ReadFederationMessage(keys._1)
    Await.ready(ask, duration).value match {
      case Some(Success(DbActor.DbActorReadFederationMessageAck(Some(message)))) =>
        val expect: FederationExpect = FederationExpect.buildFromJson(message.data.toString)
        val challengeMessage: Message = expect.challenge
        val expectedChallenge: FederationChallenge = FederationChallenge.buildFromJson(challengeMessage.data.toString)

        val ask = for {
          case (_, message, Some(data)) <- extractParameters[FederationChallenge](rpcMessage, serverUnexpectedAnswer)
        } yield data

        Await.ready(ask, duration).value match {
          case Some(Success(data)) =>
            if (data.value.equals(expectedChallenge.value) && data.validUntil == expectedChallenge.validUntil)
              federationResult = FederationResult(status._1, expect.publicKey, challengeMessage)
            else
              federationResult = FederationResult(status._2, reason, challengeMessage)

            // TODO SEND THE RESULT TO THE OTHER SERVER

            // after sending the result, we delete both the challenge and expect messages from the db
            val combined = for {
              _ <- dbActor ? DbActor.DeleteFederationMessage(keys._3)
              _ <- dbActor ? DbActor.DeleteFederationMessage(keys._1)
            } yield ()
            Await.ready(combined, duration).value match {
              case Some(Success(_)) => Right(rpcMessage)
              case _                => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"Couldn't delete both federationChallenge and federationExpect messages", rpcMessage.getId))
            }

          case _ => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"Couldn't extract federationChallenge parameters", rpcMessage.getId))
        }

      case Some(Success(DbActor.DbActorReadFederationMessageAck(None))) => Left(PipelineError(
          ErrorCodes.SERVER_ERROR.id,
          s"federationExpect is not found in the db",
          rpcMessage.getId
        ))
      case _ => Left(PipelineError(
          ErrorCodes.SERVER_ERROR.id,
          s"Couldn't obtain federationExpect message",
          rpcMessage.getId
        ))
    }

  }

  def handleFederationChallengeRequest(rpcMessage: JsonRpcRequest): GraphMessage = {
    var challengeValue = ""
    val result = generateRandomBytes(CHALLENGE_NB_BYTES)
    result match {
      case Success(bytes) => challengeValue = Base16Data.byteArrayToHexString(bytes)
      case Failure(_)     => return Left(PipelineError(ErrorCodes.INVALID_ACTION.id, s"handleFederationChallengeRequest failed: couldn't generate challenge", rpcMessage.getId))
    }

    val timestamp = Timestamp(Instant.now().plus(5, ChronoUnit.MINUTES).getEpochSecond)
    val challenge: FederationChallenge = FederationChallenge(Base16Data(challengeValue), timestamp)

    val serverKeys = retrieveServerKeys(dbActor)
    serverKeys match {
      case Some((publicKey: PublicKey, privateKey: PrivateKey)) =>
        val federationChannel: Channel = rpcMessage.getParamsChannel
        val challengeData: Base64Data = Base64Data.encode(challenge.toJson.toString)
        val challengeSignature: Signature = privateKey.signData(challengeData)
        val Id: Hash = Hash.fromStrings(challengeData.toString, challengeSignature.toString)
        val challengeMessage: Message = Message(challengeData, publicKey, challengeSignature, Id, List.empty)

        val challengeKey: String = challengeValue + timestamp.toString
        val combined = for {
          _ <- dbActor ? DbActor.WriteFederationMessage(keys._3, challengeMessage)
          _ <- mediator ? PubSubMediator.Propagate(federationChannel, challengeMessage)
        } yield ()
        Await.ready(combined, duration).value match {
          case Some(Success(_)) => Right(rpcMessage)
          case _                => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleChallengeRequest unknown error", rpcMessage.getId))
        }

      case _ => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"failed to retrieve server keys", rpcMessage.getId))
    }

    // Right(rpcMessage)
    // the implementation of broadcast sends the whole message already not only the jsValue
    // Await.result(
    // broadcast(rpcMessage, federationChannel, challenge.toJson, federationChannel),
    // duration

  }
  def handleFederationInit(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask = for {
      case (_, message, Some(data)) <- extractParameters[FederationInit](rpcMessage, serverUnexpectedAnswer)
    } yield (message, data)

    Await.ready(ask, duration).value match {
      case Some(Success(message, data)) =>
        val challengeMessage: Message = data.challenge // already signed by Alice
        val askWrite = dbActor ? DbActor.WriteFederationMessage(keys._2, message)
        Await.ready(askWrite, duration).value match {
          case Some(Success(_)) => // TODO: send the challengeMessage to the other server, how
            Right(rpcMessage)

          case _ => Left(PipelineError(
              ErrorCodes.SERVER_ERROR.id,
              s"Couldn't store FederationInit message in the db",
              rpcMessage.getId
            ))
        }
      // val remoteFederationChannel = generateFederationChannel(data.laoId)
      // val remoteServer = data.serverAddress

      // broadcast(rpcMessage, rpcMessage.getParamsChannel,FederationChallengeFormat.write(challenge), remoteFederationChannel )

      case _ => Left(PipelineError(
          ErrorCodes.SERVER_ERROR.id,
          s"Couldn't extract federationInit parameters",
          rpcMessage.getId
        ))
    }
  }

  def handleFederationExpect(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask = for {
      case (_, message, Some(data)) <- extractParameters[FederationExpect](rpcMessage, serverUnexpectedAnswer)
    } yield (message, data)

    // TODO: CHECK IF THE CHALLENGE IN THE EXPECT MSG IS ONE OF THE CHALLENGES GENERATED BY THE SERVER OR NOT IN THE VALIDATOR
    Await.ready(ask, duration).value match {
      case Some(Success(message, data)) =>
        val ask = dbActor ? DbActor.WriteFederationMessage(keys._1, message)
        Await.ready(ask, duration).value match {
          case Some(Success(_)) => Right(rpcMessage)
          case _ => Left(PipelineError(
              ErrorCodes.SERVER_ERROR.id,
              s"Couldn't store federationExpect message in the db",
              rpcMessage.getId
            ))
        }

      case _ => Left(PipelineError(
          ErrorCodes.SERVER_ERROR.id,
          s"Couldn't extract federationExpect parameters",
          rpcMessage.getId
        ))
    }
  }

  def handleFederationResult(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask = for {
      case (_, message, Some(data)) <- extractParameters[FederationResult](rpcMessage, serverUnexpectedAnswer)
    } yield (message, data)

    Await.ready(ask, duration).value match {
      case Some(Success(message, data)) =>
        // do we store the result in the db ?
        val combined = for {
          _ <- mediator ? PubSubMediator.Propagate(rpcMessage.getParamsChannel, message)
          _ <- dbActor ? DbActor.DeleteFederationMessage(keys._2)
        } yield ()

        Await.ready(combined, duration).value match {
          case Some(Success(_)) => Right(rpcMessage)
          // case Some(Success(_)) if data.status.equals(status._2) => Left(PipelineError(ErrorCodes.INVALID_ACTION.id, s"failed to federate", rpcMessage.getId))
          case _ => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleChallengeResult unknown error", rpcMessage.getId))
        }
      case _ => Left(PipelineError(
          ErrorCodes.SERVER_ERROR.id,
          s"Couldn't extract federationResult parameters",
          rpcMessage.getId
        ))

    }
  }

  private def generateRandomBytes(numBytes: Int): Try[Array[Byte]] = {
    val randomBytes = new Array[Byte](numBytes)
    val secureRandom = new SecureRandom()

    Try {
      secureRandom.nextBytes(randomBytes)
      randomBytes
    }
  }

  private def generateFederationChannel(laoId: Hash): Channel =
    Channel(Channel.ROOT_CHANNEL_PREFIX + laoId + Channel.FEDERATION_CHANNEL_PREFIX)

  private def retrieveServerKeys(dbActor: AskableActorRef): Option[(PublicKey, PrivateKey)] =
    val ask =
      for {
        publicKey <- dbActor ? DbActor.ReadServerPublicKey()
        privateKey <- dbActor ? DbActor.ReadServerPrivateKey()
      } yield (publicKey, privateKey)

    val serverKeys = Await.ready(ask, duration).value.get match {
      case Success(DbActor.DbActorReadServerPublicKeyAck(publicKey), DbActor.DbActorReadServerPrivateKeyAck(privateKey)) => Some((publicKey, privateKey))
      case _                                                                                                             => None
    }
    serverKeys
}
