package ch.epfl.pop.pubsub.graph.handlers

import akka.actor.ActorRef
import akka.pattern.AskableActorRef
import ch.epfl.pop.decentralized.ConnectionMediator
import ch.epfl.pop.decentralized.ConnectionMediator.GetFederationServer
import ch.epfl.pop.json.MessageDataProtocol.*
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.MethodType.publish
import ch.epfl.pop.model.network.method.ParamsWithMessage
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.federation.{FederationChallenge, FederationExpect, FederationInit, FederationResult}
import ch.epfl.pop.model.objects.*
import ch.epfl.pop.pubsub.ClientActor.ClientAnswer
import ch.epfl.pop.pubsub.PubSubMediator
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
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
  final lazy val handlerInstance = new FederationHandler(super.dbActor, super.mediator, super.connectionMediator)
  
  def handleFederationChallengeRequest(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleFederationChallengeRequest(rpcMessage)

  def handleFederationExpect(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleFederationExpect(rpcMessage)

  def handleFederationInit(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleFederationInit(rpcMessage)

  def handleFederationChallenge(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleFederationChallenge(rpcMessage)

  def handleFederationResult(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleFederationResult(rpcMessage)

}

class FederationHandler(dbRef: => AskableActorRef, mediatorRef: => AskableActorRef, connectionMediatorRef: => AskableActorRef) extends MessageHandler {

  override final val dbActor: AskableActorRef = dbRef
  override final val mediator: AskableActorRef = mediatorRef
  override final val connectionMediator: AskableActorRef = connectionMediatorRef

  private final val CHALLENGE_NB_BYTES: Int = 32
  private val serverUnexpectedAnswer: String = "The server is doing something unexpected"
  private final val reason: String = "Incorrect challenge"
  private final val status: (String, String) = ("success", "failure")
  private final val keys: (String, String, String) = ("expect", "init", "challenge")

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
        val challengeMessage: Message = constructMessage(challenge.toJson.toString, privateKey, publicKey)

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

  }

  def handleFederationExpect(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask = for {
      case (_, message, Some(data)) <- extractParameters[FederationExpect](rpcMessage, serverUnexpectedAnswer)
    } yield (message, data)

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

  def handleFederationInit(rpcMessage: JsonRpcRequest): GraphMessage = {
    val ask = for {
      case (_, message, Some(data)) <- extractParameters[FederationInit](rpcMessage, serverUnexpectedAnswer)
    } yield (message, data)

    Await.ready(ask, duration).value match {
      case Some(Success(message, data)) =>
        val challengeMessage: Message = data.challenge // already signed by Alice
        val serverAddress = data.serverAddress
        val remoteFederationChannel = generateFederationChannel(data.laoId)

        val askWrite = dbActor ? DbActor.WriteFederationMessage(keys._2, message)
        Await.ready(askWrite, duration).value match {
          case Some(Success(_)) =>
            val getServer = connectionMediator ? GetFederationServer(serverAddress)
            Await.result(getServer, duration) match {
              case ConnectionMediator.GetFederationServerAck(federationServerRef) =>
                constructAndSendRpc(remoteFederationChannel, challengeMessage, federationServerRef)
                Right(rpcMessage)

              case ConnectionMediator.NoPeer() => Left(PipelineError(
                  ErrorCodes.SERVER_ERROR.id,
                  s"No server to send the challenge to",
                  rpcMessage.getId
                ))
            }

          case _ => Left(PipelineError(
              ErrorCodes.SERVER_ERROR.id,
              s"Couldn't store FederationInit message in the db",
              rpcMessage.getId
            ))
        }

      case _ => Left(PipelineError(
          ErrorCodes.SERVER_ERROR.id,
          s"Couldn't extract federationInit parameters",
          rpcMessage.getId
        ))
    }
  }

  def handleFederationChallenge(rpcMessage: JsonRpcRequest): GraphMessage = {
    var federationResult: FederationResult = null

    val ask = dbActor ? DbActor.ReadFederationMessage(keys._1)
    Await.ready(ask, duration).value match {
      case Some(Success(DbActor.DbActorReadFederationMessageAck(Some(message)))) =>
        val expect: FederationExpect = FederationExpect.buildFromJson(message.data.decodeToString())
        val challengeMessage: Message = expect.challenge
        val expectedChallenge: FederationChallenge = FederationChallenge.buildFromJson(challengeMessage.data.decodeToString())
        val serverAddress = expect.serverAddress
        val remoteFederationChannel = generateFederationChannel(expect.laoId)

        val ask = for {
          case (_, message, Some(data)) <- extractParameters[FederationChallenge](rpcMessage, serverUnexpectedAnswer)
        } yield data

        Await.ready(ask, duration).value match {
          case Some(Success(data)) =>
            if (data.value.equals(expectedChallenge.value) && data.validUntil == expectedChallenge.validUntil)
              federationResult = FederationResult(status._1, expect.publicKey, challengeMessage)
            else
              federationResult = FederationResult(status._2, reason, challengeMessage)

            constructAndSendResult(rpcMessage, federationResult, serverAddress, remoteFederationChannel)

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

  private def generateFederationChannel(laoId: Hash): Channel = {
    Channel(Channel.ROOT_CHANNEL_PREFIX + laoId + Channel.FEDERATION_CHANNEL_PREFIX)
  }

  private def retrieveServerKeys(dbActor: AskableActorRef): Option[(PublicKey, PrivateKey)] = {
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

  private def constructMessage(data: String, privateKey: PrivateKey, publicKey: PublicKey): Message = {
    val messageData: Base64Data = Base64Data.encode(data)
    val signature: Signature = privateKey.signData(messageData)
    val messageId: Hash = Hash.fromStrings(messageData.toString, signature.toString)
    val message: Message = Message(messageData, publicKey, signature, messageId, List.empty)
    message
  }

  private def constructAndSendRpc(channel: Channel, message: Message, serverRef: ActorRef): Unit = {
    val messageParams: ParamsWithMessage = new ParamsWithMessage(channel, message)
    val challengeRpc: JsonRpcRequest = JsonRpcRequest(RpcValidator.JSON_RPC_VERSION, publish, messageParams, None)
    serverRef ! ClientAnswer(Right(challengeRpc))

  }

  private def constructAndSendResult(rpcMessage: JsonRpcRequest, federationResult: FederationResult, serverAddress: String, remoteFederationChannel: Channel): GraphMessage = {
    val serverKeys = retrieveServerKeys(dbActor)
    serverKeys match {
      case Some((publicKey: PublicKey, privateKey: PrivateKey)) =>
        val resultMessage: Message = constructMessage(federationResult.toJson.toString, privateKey, publicKey)

        val getServer = connectionMediator ? GetFederationServer(serverAddress)
        Await.result(getServer, duration) match {
          case ConnectionMediator.GetFederationServerAck(federationServerRef) =>
            constructAndSendRpc(remoteFederationChannel, resultMessage, federationServerRef)

            // after sending the result, we delete both the challenge and expect messages from the db
            val combined = for {
              _ <- dbActor ? DbActor.DeleteFederationMessage(keys._3)
              _ <- dbActor ? DbActor.DeleteFederationMessage(keys._1)
            } yield ()
            Await.ready(combined, duration).value match {
              case Some(Success(_)) => Right(rpcMessage)
              case _                => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"Couldn't delete both federationChallenge and federationExpect messages", rpcMessage.getId))
            }

          case ConnectionMediator.NoPeer() => Left(PipelineError(
              ErrorCodes.SERVER_ERROR.id,
              s"No server to send the result to",
              rpcMessage.getId
            ))
        }

      case _ => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"failed to retrieve server keys", rpcMessage.getId))
    }
  }
}
