package ch.epfl.pop.config

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}

import scala.reflect.io.Directory
import scala.sys.SystemProperties

/** RuntimeConfiguration object provider This object provides application config for setting up akka http/actor environment
  *
  * @see
  *   [[config/application.conf]]
  * @see
  *   [[ServerConf]]
  */
object RuntimeEnvironment {

  private lazy val sp = new SystemProperties()

  private def getConfDir: String = {
    /* Get config directory path from JVM */
    if (sp("clean") != null) {
      // starting the program with fresh database
      println("Starting the server without any previous persistent state")

      // removing database folder
      val directory = new Directory(new File("database"))
      if (directory.deleteRecursively()) {
        println("Removed old database folder")
      }
    }

    val virtualMachineParam = "scala.config"
    val pathConfig = sp(virtualMachineParam)
    if (pathConfig != null && !pathConfig.trim.isEmpty) {
      pathConfig.trim
    } else {
      throw new RuntimeException(s"-D$virtualMachineParam was not provided.")
    }
  }

  private lazy val appConfFile = getConfDir + File.separator + "application.conf"

  lazy val appConf: Config = ConfigFactory.parseFile(new File(appConfFile))

}
