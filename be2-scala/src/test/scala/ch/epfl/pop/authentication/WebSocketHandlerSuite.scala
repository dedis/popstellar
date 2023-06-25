package ch.epfl.pop.authentication

import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import ch.epfl.pop.config.RuntimeEnvironment
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class WebSocketHandlerSuite extends AnyFunSuite with Matchers with ScalatestRouteTest {

  test("First client connected receives messages from the second one") {
    val websocketRoute = WebSocketHandler.buildRoute(RuntimeEnvironment.serverConf)
    val path = "/response/xyz/authentication/abc/123"

    val firstClient = WSProbe.apply()
    val secondClient = WSProbe.apply()

    val message = "IdToken"

    WS(path, firstClient.flow) ~> websocketRoute ~> check {
      isWebSocketUpgrade shouldEqual true

      WS(path, secondClient.flow) ~> websocketRoute ~> check {
        isWebSocketUpgrade shouldEqual true

        secondClient.sendMessage(message)
        firstClient.expectMessage(message)

        firstClient.expectCompletion()
      }
    }
  }

  test("Cannot connect on wrong path") {
    val websocketRoute = WebSocketHandler.buildRoute(RuntimeEnvironment.serverConf)

    val wrongPaths = List(
      "response",
      "response/xyz",
      "response/xyz/authentication",
      "response/xyz/authentication/abc",
      "response/xyz/wrong/abc/123",
      "wrong/xyz/authentication/abc/123"
    )

    for (path <- wrongPaths) {
      val client = WSProbe.apply()
      WS(path, client.flow) ~> websocketRoute ~> check {
        handled shouldEqual false
      }
    }
  }
}
