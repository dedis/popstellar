package ch.epfl.pop.authentication

import akka.http.scaladsl.model.StatusCodes.{Found, OK}
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class AuthenticateSuite extends AnyFunSuite with Matchers with ScalatestRouteTest {

  private val routeName = "authenticate/"

  private val responseType = "id_token token"
  private val clientID = "abc"
  private val redirectUri = "https://wikipedia.org"
  private val scope = "openid profile"
  private val state = "some_state"
  private val responseMode = "query"
  private val loginHint = "some_hint"
  private val nonce = "1234"

  private val parametersNames = List("response_type", "client_id", "redirect_uri", "scope", "state", "response_mode", "login_hint", "nonce")

  private def buildRequest(params: List[(String, String)]): HttpRequest = {
    val parameters = params.filter(pair => pair._2.nonEmpty).toMap
    Get().withUri(Uri(routeName).withQuery(Query(parameters)))
  }

  private def buildRequest(response_type: String, client_id: String, redirect_uri: String, scope: String, state: String, response_mode: String, login_hint: String, nonce: String): HttpRequest = {
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
      responseType,
      clientID,
      redirectUri,
      scope,
      state,
      responseMode,
      loginHint,
      nonce
    )

    request ~> route ~> check {
      status shouldBe OK
    }
  }

  test("good requests without optional params succeeds") {
    val route = Authenticate.buildRoute()
    val request = buildRequest(
      responseType,
      clientID,
      redirectUri,
      scope,
      "",
      "",
      loginHint,
      nonce
    )

    println(request)

    request ~> route ~> check {
      status shouldBe OK
    }
  }

  test("invalid response type fails request") {
    val badResponseType = "invalid"

    val route = Authenticate.buildRoute()
    val request = buildRequest(
      badResponseType,
      clientID,
      redirectUri,
      scope,
      state,
      responseMode,
      loginHint,
      nonce
    )

    request ~> route ~> check {
      status shouldBe Found
      getAttributeValue(response, "error") shouldBe Some("unsupported_response_type")
    }
  }

  test("invalid scope fails request") {
    val badScope = "invalid x y"

    val route = Authenticate.buildRoute()
    val request = buildRequest(
      responseType,
      clientID,
      redirectUri,
      badScope,
      state,
      responseMode,
      loginHint,
      nonce
    )

    request ~> route ~> check {
      status shouldBe Found
      getAttributeValue(response, "error") shouldBe Some("invalid_scope")
    }
  }

  test("missing parameter fails request") {
    val route = Authenticate.buildRoute()

    val mandatoryNames = parametersNames.filter(name => name != "state" && name != "response_mode")
    val mandatoryValues = List(responseType, clientID, redirectUri, scope, loginHint, nonce)
    val mandatoryParams = mandatoryNames.zip(mandatoryValues)

    mandatoryNames.foreach { name =>
      val subParams = mandatoryParams.filter(_._1 != name)
      val request = buildRequest(subParams)

      request ~> route ~> check {
        status shouldBe Found
        getAttributeValue(response, "error") shouldBe Some("invalid_request")
        getAttributeValue(response, "error_description") shouldBe Some(s"Missing parameters: [$name]")
      }
    }
  }

  test("invalid redirect uri fails request") {
    val badRedirectUri1 = "https:google.com"
    val badRedirectUri2 = "www.cool.com"

    val route = Authenticate.buildRoute()
    val request1 = buildRequest(
      responseType,
      clientID,
      badRedirectUri1,
      scope,
      state,
      responseMode,
      loginHint,
      nonce
    )

    request1 ~> route ~> check {
      status shouldBe Found
      getAttributeValue(response, "error") shouldBe Some("invalid_request")
    }

    val request2 = buildRequest(
      responseType,
      clientID,
      badRedirectUri2,
      scope,
      state,
      responseMode,
      loginHint,
      nonce
    )

    request2 ~> route ~> check {
      status shouldBe Found
      getAttributeValue(response, "error") shouldBe Some("invalid_request")
    }
  }
}
