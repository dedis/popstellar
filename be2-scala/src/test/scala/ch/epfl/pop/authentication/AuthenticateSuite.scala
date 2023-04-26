package ch.epfl.pop.authentication

import akka.http.scaladsl.model.StatusCodes.{Found, OK}
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class AuthenticateSuite extends AnyFunSuite with Matchers with ScalatestRouteTest {

  private val routeName = "authenticate/"

  private val goodResponseType = "id_token token"
  private val badResponseType = "invalid"

  private val goodScope = "openid profile"
  private val badScope = "invalid x y"

  private val clientID = "abc"
  private val redirect_uir = "https://wikipedia.org"
  private val state = "some_state"
  private val response_mode = "query"
  private val login_hint = "some_hint"
  private val nonce = "1234"

  private val parametersNames = List("response_type", "client_id", "redirect_uri", "scope", "state", "response_mode", "login_hint", "nonce")

  private def buildRequest(params: List[(String,String)]): HttpRequest = {
    val parameters = params.filter(pair => pair._2.nonEmpty).toMap
    Get().withUri(Uri(routeName).withQuery(Query(parameters)))
  }

  private def buildRequest(response_type: String, client_id: String, redirect_uri: String, scope: String,
                   state: String, response_mode: String, login_hint: String, nonce: String): HttpRequest = {
    val params = parametersNames.zip(
      List(response_type, client_id, redirect_uri, scope, state, response_mode, login_hint, nonce)
    )
    buildRequest(params)
  }

  private def getAttributeValue(response: HttpResponse, key: String) = {
    val attributes = response.attributes
    val attributesWithName = attributes.map(pair => pair._1.name -> pair._2)
    attributesWithName.get(key)
  }

  test("good requests succeeds") {
    val route = Authenticate.buildRoute()
    val request = buildRequest(
      goodResponseType, clientID, redirect_uir, goodScope, state, response_mode, login_hint, nonce
    )

    request ~> route ~> check {
      status shouldBe OK
    }
  }

  test("good requests without optional params succeeds") {
    val route = Authenticate.buildRoute()
    val request = buildRequest(
      goodResponseType, clientID, redirect_uir, goodScope, "", "", login_hint, nonce
    )

    println(request)

    request ~> route ~> check {
      status shouldBe OK
    }
  }

  test("invalid response type fails request") {
    val route = Authenticate.buildRoute()
    val request = buildRequest(
      badResponseType, clientID, redirect_uir, goodScope, state, response_mode, login_hint, nonce
    )

    request ~> route ~> check {
      status shouldBe Found
      getAttributeValue(response, "error") shouldBe Some("unsupported_response_type")
    }
  }

  test("invalid scope fails request") {
    val route = Authenticate.buildRoute()
    val request = buildRequest(
      goodResponseType, clientID, redirect_uir, badScope, state, response_mode, login_hint, nonce
    )

    request ~> route ~> check {
      status shouldBe Found
      getAttributeValue(response, "error") shouldBe Some("invalid_scope")
    }
  }

  test("missing parameter fails request") {
    val route = Authenticate.buildRoute()

    val mandatoryNames = parametersNames.filter(name => name != "state" && name != "response_mode")
    val mandatoryValues = List(goodResponseType, clientID, redirect_uir, goodScope, login_hint, nonce)
    val mandatoryParams = mandatoryNames.zip(mandatoryValues)

    mandatoryNames.foreach{ name =>
      val subParams = mandatoryParams.filter(_._1 != name)
      val request = buildRequest(subParams)

      request ~> route ~> check {
        status shouldBe Found
        getAttributeValue(response, "error") shouldBe Some("invalid_request")
        getAttributeValue(response, "error_description") shouldBe Some(s"Missing parameters: [$name]")
      }
    }
  }
}
