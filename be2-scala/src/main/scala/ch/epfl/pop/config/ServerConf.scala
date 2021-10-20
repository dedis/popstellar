package ch.epfl.pop.config

import com.typesafe.config.Config

/**
    Simple server configuration interface 
    @see [[config/application.config]]
    @see [[RuntimeEnvironement]]

*/
object ServerConf {

  def apply(appConf: Config) = {
    val serverConf = appConf.getConfig("ch-epfl-pop-Server").getConfig("http")
    val serverInterface = serverConf.getString("interface")
    val serverPort = serverConf.getInt("port")

    new ServerConf(serverInterface, serverPort)
  }

}

/* Note: Can be upgraded for future configs :) */
case class ServerConf(interface: String, port: Int)
