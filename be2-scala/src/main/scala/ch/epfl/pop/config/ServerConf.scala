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

    new ServerConf(serverInterface, serverPort, clientPath, serverPath, authenticationPath)
  }

}

/* Note: Can be upgraded for future configs :) */
final case class ServerConf(interface: String, port: Int, clientPath: String, serverPath: String, authenticationPath: String)
