package ch.epfl.pop.config

import java.io.File
import com.typesafe.config.{Config, ConfigFactory}
import scala.sys.SystemProperties
import akka.event.slf4j.Logger
import java.util.logging
import java.io.IOException
import java.util.regex.Pattern

/** RuntimeConfiguration object provider This object provides application config
  * for setting up akka http/actor environemnt
  *
  * @see
  *   [[config/application.conf]]
  * @see
  *   [[ServerConf]]
  */
object RuntimeEnvironment {

  private lazy val sp = new SystemProperties()

  private def getConfDir: String = {
   
   /*Get config directory path form JVM*/
    val virtualMachineParam = "scala.config"
    val path_config = sp(virtualMachineParam)
    if (path_config != null && !path_config.trim.isEmpty()) path_config.trim
    else
      throw new RuntimeException(s"-D$virtualMachineParam was not provided.")

    
   
  }

  private lazy val appConfFile =
    getConfDir + File.separator + "application.conf"

  lazy val appConf: Config = ConfigFactory.parseFile(new File(appConfFile))

}
