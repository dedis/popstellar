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
  private val redirectUri = "https://example.com"
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

  test("valid request succeeds") {
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

  test("valid request without optional params succeeds") {
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

    request ~> route ~> check {
      status shouldBe OK
    }
  }

  test("invalid response type fails the request") {
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
      getAttributeValue(response, "error") shouldBe Some(Authenticate.UNSUPPORTED_RESPONSE_TYPE_ERROR)
    }
  }

  test("invalid scope fails the request") {
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
      getAttributeValue(response, "error") shouldBe Some(Authenticate.INVALID_SCOPE_ERROR)
    }
  }

  test("any missing parameter fails the request") {
    val route = Authenticate.buildRoute()

    val mandatoryNames = parametersNames.filter(name => name != "state" && name != "response_mode")
    val mandatoryValues = List(responseType, clientID, redirectUri, scope, loginHint, nonce)
    val mandatoryParams = mandatoryNames.zip(mandatoryValues)

    for (paramToRemove <- mandatoryParams) {
      val paramsLeft = mandatoryParams.filter(_ != paramToRemove)
      val request = buildRequest(paramsLeft)

      request ~> route ~> check {
        status shouldBe Found
        getAttributeValue(response, "error") shouldBe Some(Authenticate.INVALID_REQUEST_ERROR)
        getAttributeValue(response, "error_description") shouldBe Some(s"Missing parameters: [${paramToRemove._1}]")
      }
    }
  }

  test("invalid redirect uri fails the request") {
    val badRedirectUris = List("https:example.com", "www.example.com")
    val route = Authenticate.buildRoute()

    for (badUri <- badRedirectUris) {
      val request = buildRequest(
        responseType,
        clientID,
        badUri,
        scope,
        state,
        responseMode,
        loginHint,
        nonce
      )

      request ~> route ~> check {
        status shouldBe Found
        getAttributeValue(response, "error") shouldBe Some(Authenticate.INVALID_REQUEST_ERROR)
      }
    }
  }

  test("all valid response modes are accepted") {
    val responseModes = List("query", "fragment")

    for (mode <- responseModes) {
      val route = Authenticate.buildRoute()
      val request = buildRequest(
        responseType,
        clientID,
        redirectUri,
        scope,
        state,
        mode,
        loginHint,
        nonce
      )

      request ~> route ~> check {
        status shouldBe OK
      }
    }
  }

  test("invalid response mode fails the request") {
    val badResponseMode = "wrong_mode"

    val route = Authenticate.buildRoute()
    val request = buildRequest(
      responseType,
      clientID,
      redirectUri,
      scope,
      state,
      badResponseMode,
      loginHint,
      nonce
    )

    request ~> route ~> check {
      status shouldBe Found
      getAttributeValue(response, "error") shouldBe Some(Authenticate.INVALID_REQUEST_ERROR)
    }
  }
}
