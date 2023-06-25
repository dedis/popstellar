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
import ch.epfl.pop.model.objects.{Base64Data, DbActorNAckException, Hash, PublicKey}
import ch.epfl.pop.pubsub.graph.validators.MessageValidator.extractData
import ch.epfl.pop.pubsub.graph.{ErrorCodes, GraphMessage, PipelineError}
import ch.epfl.pop.storage.DbActor._
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm

import java.io.File
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.util.{Calendar, Date}
import javax.crypto.spec.SecretKeySpec
import scala.concurrent.Await
import scala.util.{Failure, Success, Try}

/** Handler for Popcha related messages
  */
object PopchaHandler extends MessageHandler {
  final lazy val handlerInstance = new PopchaHandler(super.dbActor)

  def handleAuthentication(rpcMessage: JsonRpcRequest): GraphMessage = handlerInstance.handleAuthentication(rpcMessage)
}

class PopchaHandler(dbRef: => AskableActorRef) extends MessageHandler {

  private val TOKEN_VALIDITY_DURATION = 3600

  def handleAuthentication(rpcMessage: JsonRpcRequest): GraphMessage = {
    val (authenticate, laoId, _, _) = extractData[Authenticate](rpcMessage)
    handleAuthentication(rpcMessage, sendResponseToWebsocket(rpcMessage, authenticate, laoId), retrievePrivateKeyFromDB)
  }

  /** Handles authentication messages
    *
    * @param rpcMessage
    *   message received
    * @return
    *   a graph message representing the message's state after handling
    */
  def handleAuthentication(rpcMessage: JsonRpcRequest, responseHandler: Uri => GraphMessage, privateKeyProvider: => RSAPrivateKey): GraphMessage = {
    val (authenticate, laoId, sender, _) = extractData[Authenticate](rpcMessage)
    for {
      _ <- registerAuthentication(rpcMessage, sender, authenticate.clientId, authenticate.identifier)
      result <- sendOpenIdToken(rpcMessage, authenticate, laoId, responseHandler, privateKeyProvider)
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

  private def generateOpenIdToken(rpcMessage: JsonRpcMessage, authenticate: Authenticate, laoId: Hash, privateKeyProvider: => RSAPrivateKey): Either[PipelineError, String] = {
    val date = Calendar.getInstance()
    val issuedTime = date.getTime
    val expTime = new Date(date.getTimeInMillis + (TOKEN_VALIDITY_DURATION * 1000)) // Expire after 1h

    val privateKey = Try(privateKeyProvider) match {
      case Success(key) => key
      case Failure(ex) => return Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleAuthentication failed : private key fetch failed with error \"${ex.getMessage}\"", rpcMessage.getId))
    }
    val algorithm = Algorithm.RSA256(privateKey)

    Try(
      JWT.create()
        .withIssuer(RuntimeEnvironment.ownRootPath)
        .withSubject(authenticate.identifier.base64Data.toString)
        .withAudience(authenticate.clientId)
        .withIssuedAt(issuedTime)
        .withExpiresAt(expTime)
        .withClaim("nonce", authenticate.nonce.decodeToString())
        .withClaim("laoId", laoId.base64Data.toString)
        .withClaim("auth_time", issuedTime)
        .sign(algorithm)
    ) match {
      case Success(jwt) => Right(jwt)
      case Failure(ex)  => Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"handleAuthentication failed : ${ex.getMessage}", rpcMessage.getId))
    }
  }

  private def sendOpenIdToken(rpcMessage: JsonRpcMessage, authenticate: Authenticate, laoId: Hash, responseFlowHandler: Uri => GraphMessage, privateKeyProvider: => RSAPrivateKey): GraphMessage = {
    val jwt = generateOpenIdToken(rpcMessage, authenticate, laoId, privateKeyProvider) match {
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

    val wsAddress = s"ws://${authenticate.popchaAddress}/${RuntimeEnvironment.serverConf.responseEndpoint}/$laoId/authentication/${authenticate.clientId}/${authenticate.nonce.decodeToString()}"

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

  private def retrievePrivateKeyFromDB: RSAPrivateKey = {
    val p = Await.ready(dbRef ? ReadServerPrivateKey(), duration).value.get match {
      case Success(DbActorReadServerPrivateKeyAck(key)) => key
      case Success(reply) => throw new RuntimeException(s"unexpected reply: $reply")
      case Failure(ex) => throw ex
    }

//    val keyStr = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQD1wuwEwZXlKmi6ahiU2yn8mUP1K7mu4aXRNjdbVFGfSdxMNDk_FNlPg6cUoDFoh37lmTvR5CSZjT-6rup7di_yfCq7O1Dk9-M8e0hXX9SB3AgqC3kF5Us3fLSKII0aSnIfGQpfui4TveQVsNV1Wz9KhhGtK5o9S2ygkVdydMZMhnomI_Rk36SjFv0vH6XeyZPxqzYrocQUm27H-LUIK-zuG2n-fT9aM1fom8S5InCw5eDOlISoxRXJUnDrGs0yOF4xwVHX8Cb4xuraicBQQbC6hg3wru1kUEETQqbZAYm7L6xlEMJA3KrA1wDdDMVB9gv8pCDsr7q3cISGKoA9idR_AgMBAAECggEAZRrLQUuo0I4JZsN2GGsvk0k0YX0bFzyinHa7AFooeCkJNdp4QKDho-osBvq-SNwRUwCe0QMUIY2wDaufMKqXICF_7OYCqifm9r9bLALzKHdubmmo4MmLj6jAl2C72_iLiYqiL26nPRzuZBQLRQwEdjLAu-bHvfa1Gjug56ft2pXb77fcmiqarmKzqBah2BbgQQP_QP2Wd8Reo-v0FqRpDeyOOs9k2cdroL-afRWMBXZXKztF7NcWPt1geK-04nRdGyr1-7EInXSxBvyDM_hQIYRJ6Xg0sczrhw5RT407JK9iIYRvvlkp8uAaCCcE1eTotLqXi_MhpQC4ADnQRhNnDQKBgQD4g_Mo5cVXg3KUSo8IzcmhclKuGvEr4Ef8idNzuSVgPUgdvbGZ0bcmyB1V94vsKjSTbwYBnrNUg86q3U5a9WOyNdF1k3giFL7i8kPVdIBoH6SqfZS0XdvjgIVg_183RNR228UwOLuq2kOKD64ZliS3bNSKhGePtJ26SMMfKhncZQKBgQD9Kb0awrqj78nWtaDWH-empvnExY_dmGtU9RSYk5bR3kbpmxn5aReOGIe0_6M4lIOPCQ_186WRYQ3LeZbSi0kmwlqQZCwrgwg2IdOpewngeFCHQyuik5jRi60whorxiu6Gr7r7hzmyteY6cdjJQitFXRmTKhZ8AwO8nqm_2MaFEwKBgQCaW0qzAlRakPigBtdkvn0YXCvinDVj7UCJKQo_fKYsaqPaZTJkug-qdO1TshgkrepOEM2IQAxe2CeLlT3P3U75J9hb0Sby9DPEPnnHoT8IbW8XvjyY8xta6T7vCm8XoxbcZJDL7NETw9HjdO3MUqenjl9NUgQJDERTqlIXj0zUyQKBgEEJK-9n_xKGU0-5MSxQ3e3OD3QhXKgMs-YLX9MidyhK9eSlV7Le8JIscxBoa8HpRTLFnTgN84a5bBXNkpVb-treKu3VDhPPgZiGcB2l4g-sWOOmudr2F9gdDczdg2wxyL0I__wF7Sif2hlBjfOF8B_NnvhTY5tQGvGlwO3r_nPJAoGAJuoQxXCFbpu0EMdKzuxZGquJnZlgpy6AAvJIRMvtBlAfcIjfDtlmDh6Z3Xpuvmp1xaNSr_KDAdr4yBO8sM573cvV7x0NO2CszFm0yOlyAftEz9djpkEHoRHze4_KbxD6-0mBURAO292SJkrVQ5X6QmLt71RmjDLbo2rIteaVdxE="
//    val bytes = Base64Data(keyStr).decode()

//    val key = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCxtH0inzi1Hi9zUgR58El0cyVABCbVSW1gTe+zzvl5jwBJniXOUsbqJVF7ssfhU3qPsYHhOOPYS2BQVQBCpsWzmaftwU7sNH2uJAOAsZ6QBDmD+YvP1CG4AaNTZqbSY8E5IEwTTwST281F8A4ZVaT18NSF1Fx5F0o5eefP5i+ph97e/PiPH1dckdkeYbbA0Hp6AKextk64SqJZUyPn7fAq3WmtQYoQAe0+oL6iUNovBnDV2J1ii0+BpkPB52RwBVJY0e86SRje4yq8oj4KmjE/66q13L20zniw1COhZ2rgshpYX+ctHLl8zYkBVsCrA9brPbdgyLgcix/C6P53xRm1AgMBAAECggEAAdcYpKR5ddwGKcTjqaTvXcwDdWeVmgfUoMwDJh2H6oEB7mvmKv4jHobxvRIw4l3MRciqIPxHKmo9aReNlSMc+6slA1/zwpvDNsEbYth0B+cYoWE9gr1zoUWDEiNcqY5sOyc2d8y4WL+h9I4egeynyf7gdIeqctE8QjQc+RjXzYLyQVBLagczxLUtVD2UH6Irdgy4VI1GkevAQU8Eso6cCAfmhOiHepi5nx7hhZTL6fxBz9BkFSNhdu40Kibwr3tDGGVms3c31hqyjUNtA+6Nhe/beYa/FZ5QB35VbuculbytXUOnxIXQ2+kgHNl+X15/YVuooJNE4XZHJT2u3rts0QKBgQDEt9rNW6cnz7nD5GrsR3Z1bieKzICacfnPS6dINIGiNGThqbWiHpuZVkMu+SKPbVAxdh4sMrqmiyuFW/wYbv8W0O5kRI3hkaRxhExnGU6iG5m0GbBvDg3fDG9B569npGzoYGhBDT2CVEUd54razGovXveqB2GeRDJfRU/6kQluwwKBgQDnQdVyPw5kZMEvWQ5KxW/86pXTfP8210t6IpDyvr/SwC2i3i83vQQruukjWll8AIQHhR8SDlHxBK/k/aTR4VvEdhkyHT4+2Z4JNBGVzT7wh2IHMDUhP5/KP+tSbAhtArWw/Bv5i+lBkmi5UYVGnOOOdH6jpGGFs/LtbzPhi1Y+JwKBgApxVB0oq2PypALhIkfutzwen9y/ZGhOeptlgbjUiLkqnNxZ3PmBNHNcX+6jbRE+FU663XktLDlhE+tdabGGWuZEKxOJjBqYV6lrA39JmaIDYxJrdrE+hr/7cgCGowoWcW2YiJBDeqtre8vNmdJpnY1sNiuBfs4fAqmKDWfYwS5vAoGBAJW3lHWjhzDN3hhGQq97xXXrddZ23U/m8LGAwXC2t7+8tY7044Lld1bManV93+Mc/l1T/PqWlMxCKZJJ+DP8/4lgoA1Gy26rOtpggGYIfBACxh87QZpl85Bf83zn/kh88Z5EieP0ha3zGKOpuGwv1E788qQFHzINf1/il6cUq2APAoGBAKntLR+/m/rwfGUXj3obZua6zW7jv31/3v/Beefr+swMX4PyKP2UNhsZpk9w/lFpssGWTJu8R7AwjBJLxdRl6ugWAxojAtVWd/lQEq2IUCejLLjX4s15syVAd46rnEyc62SCvPEWnjhQy6RzP/+qzSHxRl1I8Q+19G/PziUDCicK"
//    val bytes = key.getBytes(StandardCharsets.UTF_8)

    import java.io.DataInputStream
    import java.io.FileInputStream
    //get the private key//get the private key

    val PRIVATE_KEY = "src/tmp/private_key.der"

    val file: File = new File(PRIVATE_KEY)
    val fis: FileInputStream = new FileInputStream(file)
    val dis: DataInputStream = new DataInputStream(fis)

    val keyBytes: Array[Byte] = new Array[Byte](file.length.asInstanceOf[Int])
    dis.readFully(keyBytes)

    println(keyBytes.mkString("Array(", ", ", ")"))

    import java.security.KeyFactory
    import java.security.spec.PKCS8EncodedKeySpec
    val spec = new PKCS8EncodedKeySpec(keyBytes)
    val kf = KeyFactory.getInstance("RSA")
    kf.generatePrivate(spec).asInstanceOf[RSAPrivateKey]

//    val spec = new SecretKeySpec(privateKey.base64Data.getBytes, "RSA")
//    val factory = KeyFactory.getInstance("RSA")
//    val rsaPrivateKey = factory.generatePrivate(spec)
//    rsaPrivateKey.asInstanceOf[RSAPrivateKey]
  }
}
