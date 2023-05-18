package ch.epfl.pop.config

import java.io.File
import com.typesafe.config.{Config, ConfigFactory}

import scala.io.Source.fromFile
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
    if (pathConfig != null && pathConfig.trim.nonEmpty) {
      pathConfig.trim
    } else {
      throw new RuntimeException(s"-D$virtualMachineParam was not provided.")
    }
  }

  private def testMode: Boolean = {
    sp("test") != null
  }

  private lazy val confDir: String = getConfDir
  private lazy val appConfFile = confDir + File.separator + "application.conf"

  lazy val isTestMode: Boolean = testMode
  lazy val serverPeersList: String =
    if (isTestMode) {
      confDir + File.separator + "server-peers-list-TEST.conf"
    } else {
      confDir + File.separator + "server-peers-list.conf"
    }

  lazy val appConf: Config = ConfigFactory.parseFile(new File(appConfFile))

  // Regex from dataGreetLao.json
  private val addressPattern = "^(ws|wss)://.*(:d{0,5})?/.*$"

  def readServerPeers(): List[String] = {
    val source =
      try {
        fromFile(serverPeersList)
      } catch {
        case ex: Throwable =>
          println("RuntimeEnvironment: " + ex.toString)
          return Nil
      }

    val addressList = source.getLines().map(_.trim).filter(_.matches(addressPattern)).toList
    source.close()

    addressList
  }

}
