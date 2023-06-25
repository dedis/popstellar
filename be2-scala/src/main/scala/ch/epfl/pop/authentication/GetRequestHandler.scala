package ch.epfl.pop.authentication

import akka.http.scaladsl.model.{AttributeKey, ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives.{complete, path}
import akka.pattern.AskableActorRef
import akka.util.Timeout
import ch.epfl.pop.config.ServerConf
import ch.epfl.pop.storage.DbActor.{DbActorReadServerPublicKeyAck, ReadServerPublicKey}

import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.util.Success

object GetRequestHandler {

  // Implicit for system actors
  implicit val timeout: Timeout = Timeout(1, TimeUnit.SECONDS)

  def buildRoutes(config: ServerConf, dbActorRef: AskableActorRef): server.Route = {
    Directives.get {
      Directives.concat(
        buildPathRoute(config.authenticationPath, Authenticate.buildRoute()),
        buildPathRoute(config.publicKeyEndpoint, fetchPublicKey(dbActorRef))
      )
    }
  }

  private def buildPathRoute(pathName: String, route: server.Route): server.Route = {
    path(pathName) {
      route
    }
  }

  private def fetchPublicKey(dbActorRef: AskableActorRef): server.Route = {
    complete {
      Await.ready(dbActorRef ? ReadServerPublicKey(), timeout.duration).value match {
        case Some(Success(DbActorReadServerPublicKeyAck(publicKey))) => requestSuccess(publicKey.base64Data.toString)
        case Some(reply) => requestFailure("Server error", reply.toString)
        case None => requestFailure("Server error", "No response received from DB")
      }
    }
  }

  private def requestFailure(error: String, errorDescription: String): HttpResponse = {
    HttpResponse(status = StatusCodes.Found)
      .addAttribute(AttributeKey("error"), error)
      .addAttribute(AttributeKey("error_description"), errorDescription)
  }

  private def requestSuccess(response: String) = {
    HttpResponse(status = StatusCodes.OK, entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, response))
  }
}
