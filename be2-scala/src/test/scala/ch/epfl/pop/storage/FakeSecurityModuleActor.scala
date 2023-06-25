package ch.epfl.pop.storage

import akka.actor.Actor
import akka.event.LoggingReceive
import ch.epfl.pop.storage.FakeSecurityModuleActor.{rsaPrivateKey, rsaPublicKey, rsaPublicKeyPem}
import ch.epfl.pop.storage.SecurityModuleActor.{ReadRsaPublicKey, ReadRsaPublicKeyAck, ReadRsaPublicKeyPem, ReadRsaPublicKeyPemAck, SignJwt, SignJwtAck}
import com.auth0.jwt.algorithms.Algorithm

import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}
import java.security.{KeyPair, KeyPairGenerator}

final case class FakeSecurityModuleActor() extends Actor {
  override def receive: Receive = LoggingReceive {
    case ReadRsaPublicKey() => sender() ! ReadRsaPublicKeyAck(rsaPublicKey)

    case ReadRsaPublicKeyPem() => sender() ! ReadRsaPublicKeyPemAck(rsaPublicKeyPem)

    case SignJwt(jwt) =>
      val algorithm = Algorithm.RSA256(rsaPrivateKey)
      sender() ! SignJwtAck(jwt.sign(algorithm))
  }
}

object FakeSecurityModuleActor {
  private val keyPair: KeyPair = {
    val generator = KeyPairGenerator.getInstance("RSA")
    generator.initialize(2048)
    generator.generateKeyPair
  }

  val rsaPublicKey: RSAPublicKey = keyPair.getPublic.asInstanceOf[RSAPublicKey]

  val rsaPrivateKey: RSAPrivateKey = keyPair.getPrivate.asInstanceOf[RSAPrivateKey]

  val rsaPublicKeyPem = "-----BEGIN PUBLIC KEY-----Some_Public_Key-----END PUBLIC KEY-----"
}
