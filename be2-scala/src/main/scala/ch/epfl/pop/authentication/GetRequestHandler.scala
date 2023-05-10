package ch.epfl.pop.authentication

import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives.path
import ch.epfl.pop.config.ServerConf

object GetRequestHandler {

  def buildRoutes(config: ServerConf): server.Route = {
    Directives.get {
      buildPathRoute(config.authenticationPath, Authenticate.buildRoute())
    }
  }

  private def buildPathRoute(pathName: String, route: server.Route): server.Route = {
    path(pathName) {
      route
    }
  }
}
