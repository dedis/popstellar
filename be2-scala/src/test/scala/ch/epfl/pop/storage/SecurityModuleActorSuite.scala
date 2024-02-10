package ch.epfl.pop.storage

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.testkit.TestKit
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.storage.SecurityModuleActor.{ReadRsaPublicKey, ReadRsaPublicKeyAck, SignJwt, SignJwtAck}
import ch.epfl.pop.storage.SecurityModuleActorSuite.testSecurityDirectory
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.{AnyFunSuiteLike => FunSuiteLike}
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Await

class SecurityModuleActorSuite extends TestKit(ActorSystem("SecurityModuleActorSystem")) with FunSuiteLike with Matchers with BeforeAndAfterAll with AskPatternConstants {
  override def afterAll(): Unit = {
    // Stops the test actor system
    TestKit.shutdownActorSystem(system)
  }

  test("Jwt signature can be verified using the server public key") {
    val securityModuleActor = system.actorOf(Props(SecurityModuleActor(testSecurityDirectory)))

    val token = JWT.create().withClaim("answer to life", "42")

    val tokenSignedAsk = Await.result(securityModuleActor ? SignJwt(token), duration)
    tokenSignedAsk shouldBe a[SignJwtAck]
    val tokenSigned = tokenSignedAsk.asInstanceOf[SignJwtAck].jwt

    val publicKeyAsk = Await.result(securityModuleActor ? ReadRsaPublicKey(), duration)
    publicKeyAsk shouldBe a[ReadRsaPublicKeyAck]
    val publicKey = publicKeyAsk.asInstanceOf[ReadRsaPublicKeyAck].publicKey

    JWT.require(Algorithm.RSA256(publicKey)).build().verify(tokenSigned)
  }
}

object SecurityModuleActorSuite {
  val testSecurityDirectory = "src/security/test"
}
