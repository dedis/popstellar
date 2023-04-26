package ch.epfl.pop.authentication

import akka.http.scaladsl.model.StatusCodes.ClientError
import akka.http.scaladsl.model.{AttributeKey, HttpRequest, HttpResponse}
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import ch.epfl.pop.authentication.Authenticate.verifyResponseType

object Authenticate {
  case class RequestParameters(response_type: String, client_id: String, redirect_uri: String, scope: String,
                               state: Option[String], response_mode: Option[String], login_hint: String, nonce: String)

  type VerificationState = Either[(String, String), Unit]

  val FAILURE_ERROR_CODE = 302

  def buildRoute(): server.Route = {
    extractRequest { request =>
      parameters(
        "response_type",
        "client_id",
        "redirect_uri",
        "scope",
        "state".optional,
        "response_mode".optional,
        "login_hint",
        "nonce"
      ) {
        (response_type, client_id, redirect_uri, scope, state, response_mode, login_hint, nonce) => {
          val params = RequestParameters(response_type, client_id, redirect_uri, scope, state, response_mode, login_hint, nonce)
          verifyParameters(params) match {
            case Left(error -> errorDescription) => complete(authenticationFailure(error, errorDescription, state))
            case Right(_) => complete(generateChallenge(request))
          }
        }
      } ~ complete(authenticationFailure("invalid_request", "invalid request parameters", None))
    }
  }

  def verifyParameters(params: RequestParameters): VerificationState = {
    for {
      _ <- verifyResponseType(params.response_type)
      result <- verifyScope(params.scope)
    } yield result
  }

  def verifyResponseType(response_type: String): VerificationState = {
    val expectedResponseType = "id_token token"
    if (response_type == expectedResponseType) Right(()) else
      Left("unsupported_response_type" -> s"expected \"$expectedResponseType\" but received \"$response_type\"")
  }

  def verifyScope(scope: String): VerificationState = {
    val expectedScope = "openid"
    val scopes = scope.split(" ")
    if (scopes.contains(expectedScope)) Right(()) else
      Left("invalid_scope" -> s"expected scope to contain \"$expectedScope\" but received \"$scope\"")
  }

  def generateChallenge(request: HttpRequest): HttpResponse =
    HttpResponse(entity= request.toString()) // TODO: add code for generating the challenge qrcode page

  def authenticationFailure(error: String, errorDescription: String, state: Option[String]): HttpResponse = {
  val response = HttpResponse(status= ClientError(FAILURE_ERROR_CODE)(error, "Found"))
    response.addAttribute(AttributeKey("error"), error)
    response.addAttribute(AttributeKey("error_description"), errorDescription)
    if (state.isDefined)
      response.addAttribute(AttributeKey("state"), state)
    response
  }
}
