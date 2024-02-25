package ch.epfl.pop.authentication

import akka.actor.Props
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.pattern.{AskableActorRef, ask}
import akka.testkit.TestKit
import ch.epfl.pop.config.ServerConf
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.storage.SecurityModuleActor
import ch.epfl.pop.storage.SecurityModuleActor.{ReadRsaPublicKeyPem, ReadRsaPublicKeyPemAck}
import ch.epfl.pop.storage.SecurityModuleActorSuite.testSecurityDirectory
import org.scalatest.funsuite.{AnyFunSuite => FunSuite}
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Await

class GetRequestHandlerSuite extends FunSuite with Matchers with AskPatternConstants with ScalatestRouteTest {

  override def afterAll(): Unit = {
    // Stops the test actor system
    TestKit.shutdownActorSystem(system)
  }

  test("Accessing public key endpoint returns the server's public key in pem format") {
    val dummyServerConf = new ServerConf("", 42, "", "", "auth", "", "publicKey", "")
    val securityModuleActorRef: AskableActorRef = system.actorOf(Props(SecurityModuleActor(testSecurityDirectory)))

    val publicKeyAsk = Await.result(securityModuleActorRef ? ReadRsaPublicKeyPem(), duration)
    val serverPublicKey = publicKeyAsk.asInstanceOf[ReadRsaPublicKeyPemAck].publicKey

    val route = GetRequestHandler.buildRoutes(dummyServerConf, securityModuleActorRef)
    val request = Get("/" + dummyServerConf.publicKeyEndpoint)

    request ~> route ~> check {
      status shouldBe OK
      responseAs[String] shouldEqual serverPublicKey
    }
  }
}
