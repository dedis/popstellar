package ch.epfl.pop.authentication

import akka.http.scaladsl.model._
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives.{complete, pathPrefix}
import akka.pattern.AskableActorRef
import akka.util.Timeout
import ch.epfl.pop.config.ServerConf
import ch.epfl.pop.storage.SecurityModuleActor.{ReadRsaPublicKeyPem, ReadRsaPublicKeyPemAck}

import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.util.Success

/** Object to handle the http-get requests supported by the server
  */
object GetRequestHandler {

  // Implicit for system actors
  implicit val timeout: Timeout = Timeout(1, TimeUnit.SECONDS)

  /** Build routes to handle the http-get requests supported by the server
    * @param config
    *   server configuration to use
    * @param securityModuleActorRef
    *   security module to use for secret keys
    * @return
    *   a route to handle get requests
    */
  def buildRoutes(config: ServerConf, securityModuleActorRef: AskableActorRef): server.Route = {
    Directives.get {
      Directives.concat(
        buildPathRoute(config.authenticationPath, Authenticate.buildRoute()),
        buildPathRoute(config.publicKeyEndpoint, fetchPublicKey(securityModuleActorRef))
      )
    }
  }

  private def buildPathRoute(pathName: String, route: server.Route): server.Route = {
    pathPrefix(pathName) {
      route
    }
  }

  private def fetchPublicKey(securityModuleActorRef: AskableActorRef): server.Route = {
    complete {
      Await.ready(securityModuleActorRef ? ReadRsaPublicKeyPem(), timeout.duration).value match {
        case Some(Success(ReadRsaPublicKeyPemAck(publicKey))) => requestSuccess(publicKey)
        case Some(reply)                                      => requestFailure("Server error", reply.toString)
        case None                                             => requestFailure("Server error", "No response received from DB")
      }
    }
  }

  private def requestFailure(error: String, errorDescription: String): HttpResponse = {
    HttpResponse(status = StatusCodes.OK)
      .addAttribute(AttributeKey("error"), error)
      .addAttribute(AttributeKey("error_description"), errorDescription)
  }

  private def requestSuccess(response: String) = {
    HttpResponse(status = StatusCodes.OK, entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, response))
  }
}
