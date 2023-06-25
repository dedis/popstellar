package ch.epfl.pop.config

import com.typesafe.config.Config

/** Simple server configuration interface
  *
  * @see
  *   [[config/application.config]]
  * @see
  *   [[RuntimeEnvironment]]
  */
object ServerConf {

  def apply(appConf: Config): ServerConf = {
    val serverConf = appConf.getConfig("ch_epfl_pop_Server").getConfig("http")
    val serverInterface = serverConf.getString("interface")
    val serverPort = serverConf.getInt("port")

    val clientPath = serverConf.getString("client-path")
    val serverPath = serverConf.getString("server-path")
    val authenticationPath = serverConf.getString("authentication-path")

    val responseEndpoint = serverConf.getString("response-endpoint")
    val publicKeyEndpoint = serverConf.getString("publicKey-endpoint")

    val internalAddress = s"ws://$serverInterface:$serverPort"
    val externalAddress = {
      try {
        val configExternalAddress = serverConf.getString("external-address")
        if (configExternalAddress.isBlank) {
          internalAddress
        } else {
          configExternalAddress
        }

      } catch {
        case _: Throwable => internalAddress
      }
    }

    new ServerConf(serverInterface, serverPort, clientPath, serverPath, authenticationPath, responseEndpoint, publicKeyEndpoint, externalAddress)
  }

}

/* Note: Can be upgraded for future configs :) */
final case class ServerConf(interface: String, port: Int, clientPath: String, serverPath: String, authenticationPath: String, responseEndpoint: String, publicKeyEndpoint: String, externalAddress: String)
