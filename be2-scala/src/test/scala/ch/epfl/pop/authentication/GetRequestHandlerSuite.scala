package ch.epfl.pop.authentication

import akka.actor.Props
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.pattern.AskableActorRef
import akka.testkit.TestKit
import ch.epfl.pop.config.ServerConf
import ch.epfl.pop.storage.FakeSecurityModuleActor
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class GetRequestHandlerSuite extends AnyFunSuite with Matchers with ScalatestRouteTest {

  override def afterAll(): Unit = {
    // Stops the test actor system
    TestKit.shutdownActorSystem(system)
  }

  test("Accessing public key endpoint returns the server's public key in pem format") {
    val dummyServerConf = new ServerConf("", 42, "", "", "auth", "", "publicKey", "")
    val securityModuleActorRef: AskableActorRef = system.actorOf(Props(FakeSecurityModuleActor()))

    val route = GetRequestHandler.buildRoutes(dummyServerConf, securityModuleActorRef)
    val request = Get("/" + dummyServerConf.publicKeyEndpoint)

    request ~> route ~> check {
      status shouldBe OK
      responseAs[String] shouldEqual FakeSecurityModuleActor.rsaPublicKeyPem
    }
  }
}
