package ch.epfl.pop.authentication

import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import ch.epfl.pop.config.RuntimeEnvironment
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class WebSocketResponseHandlerSuite extends AnyFunSuite with Matchers with ScalatestRouteTest {

  test("Can connect on correct path") {
    val websocketRoute = WebSocketResponseHandler.buildRoute(RuntimeEnvironment.serverConf)
    val path = "/response/xyz/authentication/abc/123"

    val firstClient = WSProbe.apply()

    WS(path, firstClient.flow) ~> websocketRoute ~> check {
      isWebSocketUpgrade shouldEqual true
    }
  }

  test("Cannot connect on wrong path") {
    val websocketRoute = WebSocketResponseHandler.buildRoute(RuntimeEnvironment.serverConf)

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
