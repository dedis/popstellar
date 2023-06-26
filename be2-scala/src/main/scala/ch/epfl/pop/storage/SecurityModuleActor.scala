package ch.epfl.pop.storage

import akka.actor.{Actor, ActorLogging, Status}
import akka.event.LoggingReceive
import ch.epfl.pop.model.objects.DbActorNAckException
import ch.epfl.pop.pubsub.graph.ErrorCodes
import ch.epfl.pop.storage.SecurityModuleActor._
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm

import java.nio.file.{Files, Paths}
import java.security.KeyFactory
import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import scala.io.Source
import scala.util.{Success, Try}

/** Actor to use for logic involving the server's private keys. Only the public keys can exit the module, on the other hand private keys must stay here and can only be used through specific endpoint
  * @param keysFolderPath
  *   folder where the keys are stored at
  */
final case class SecurityModuleActor(keysFolderPath: String) extends Actor with ActorLogging {

  private val rsaPrivateKey: RSAPrivateKey = {
    val keyBytes = readBytes(keysFolderPath + "/private_key.der")
    val spec = new PKCS8EncodedKeySpec(keyBytes)
    val kf = KeyFactory.getInstance("RSA")
    kf.generatePrivate(spec).asInstanceOf[RSAPrivateKey]
  }

  private val rsaPublicKey: RSAPublicKey = {
    val keyBytes = readBytes(keysFolderPath + "/public_key.der")
    val spec = new X509EncodedKeySpec(keyBytes)
    val kf = KeyFactory.getInstance("RSA")
    kf.generatePublic(spec).asInstanceOf[RSAPublicKey]
  }

  private val rsaPublicKeyPem: String = {
    val source = Source.fromFile(keysFolderPath + "/public_key.pem")
    val res = source.getLines().mkString("\n")
    source.close()
    res
  }

  private def readBytes(filePath: String): Array[Byte] = {
    Files.readAllBytes(Paths.get(filePath))
  }

  private def signJwt(jwt: JWTCreator.Builder): String = {
    val algorithm = Algorithm.RSA256(rsaPrivateKey)
    jwt.sign(algorithm)
  }

  override def receive: Receive = LoggingReceive {
    case ReadRsaPublicKey() =>
      log.info(s"Actor $self (SecurityModuleActor) received a ReadRsaPublicKey request")
      sender() ! ReadRsaPublicKeyAck(rsaPublicKey)

    case ReadRsaPublicKeyPem() =>
      log.info(s"Actor $self (SecurityModuleActor) received a ReadRsaPublicKeyPem request")
      sender() ! ReadRsaPublicKeyPemAck(rsaPublicKeyPem)

    case SignJwt(jwt) =>
      log.info(s"Actor $self (SecurityModuleActor) received a SignJwt request for jwt $jwt")
      Try(signJwt(jwt)) match {
        case Success(jwtStr) => sender() ! SignJwtAck(jwtStr)
        case failure         => sender() ! failure.recover(Status.Failure(_))
      }

    case m =>
      log.info(s"Actor $self (SecurityModuleActor) received an unknown message")
      sender() ! Status.Failure(DbActorNAckException(ErrorCodes.INVALID_ACTION.id, s"securityModuleActor actor received a message '$m' that it could not recognize"))
  }
}

/** Events to use for interacting with the SecurityModuleActor
  */
object SecurityModuleActor {
  sealed trait Event

  /** Reads the rsa public key owned. Replies with [[ReadRsaPublicKeyAck]] if successful
    */
  case class ReadRsaPublicKey() extends Event

  /** Reads the rsa public key stored as a .pem file (pem format). Replies with [[ReadRsaPublicKeyPemAck]] if successful
    */
  case class ReadRsaPublicKeyPem() extends Event

  /** Sign the jwt being build using the rsa private key owned. Replies with [[SignJwtAck]] if successful
    */
  case class SignJwt(jwt: JWTCreator.Builder) extends Event

  /** Response for [[ReadRsaPublicKey]]
    * @param publicKey
    *   rsa public key owned
    */
  case class ReadRsaPublicKeyAck(publicKey: RSAPublicKey) extends Event

  /** Response for [[ReadRsaPublicKeyPem]]
    * @param publicKey
    *   rsa public key owned (in a pem format)
    */
  case class ReadRsaPublicKeyPemAck(publicKey: String) extends Event

  /** Response for [[SignJwt]]
    * @param jwt
    *   string representation of the jwt signed using the rsa private key owned
    */
  case class SignJwtAck(jwt: String) extends Event
}
