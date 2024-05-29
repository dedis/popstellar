package ch.epfl.pop.authentication

import akka.http.scaladsl.model.ContentTypes
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class QRCodeChallengeGeneratorSuite extends AnyFunSuite with Matchers {
  test("qrcode challenge is an html entity") {
    val content = "some text"
    val challenge = QRCodeChallengeGenerator.generateChallengeContent(content, "example.com", "123", "xyz", "42")
    challenge.contentType shouldBe ContentTypes.`text/html(UTF-8)`
  }
}
