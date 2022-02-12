package ch.epfl.pop.config

import com.typesafe.config.Config

/**
 * Simple server configuration interface
 *
 * @see [[config/application.config]]
 * @see [[RuntimeEnvironment]]
 *
 */
object ServerConf {

  def apply(appConf: Config): ServerConf = {
    val serverConf = appConf.getConfig("ch_epfl_pop_Server").getConfig("http")
    val serverInterface = serverConf.getString("interface")
    val serverPort = serverConf.getInt("port")
    val path = serverConf.getString("path")

    new ServerConf(serverInterface, serverPort, path)
  }

}

/* Note: Can be upgraded for future configs :) */
case class ServerConf(interface: String, port: Int, path: String)
