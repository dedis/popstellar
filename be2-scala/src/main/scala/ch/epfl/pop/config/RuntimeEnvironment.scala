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

  // Regex from dataGreetLao.json
  private val addressPattern = "^(ws|wss)://.*(:d{0,5})?/.*$"

  private lazy val sp = new SystemProperties()
  private lazy val confDir: String = getConfDir
  private lazy val appConfFile = confDir + File.separator + "application.conf"

  lazy val dbPath: String = getDbDirectory
  lazy val appConf: Config = ConfigFactory.parseFile(new File(appConfFile))
  lazy val serverConf: ServerConf = ServerConf(appConf)
  lazy val ownClientAddress = f"ws://${serverConf.interface}:${serverConf.port}/${serverConf.clientPath}"
  lazy val ownServerAddress = f"ws://${serverConf.interface}:${serverConf.port}/${serverConf.serverPath}"
  lazy val ownAuthAddress = f"http://${serverConf.interface}:${serverConf.port}/${serverConf.authenticationPath}"
  lazy val isTestMode: Boolean = testMode

  lazy val serverPeersListPath: String =
    if (isTestMode) {
      confDir + File.separator + "server-peers-list-mock.conf"
    } else {
      confDir + File.separator + "server-peers-list.conf"
    }

  def readServerPeers(): List[String] = {
    val source =
      try {
        fromFile(serverPeersListPath)
      } catch {
        case ex: Throwable =>
          println("RuntimeEnvironment: " + ex.toString)
          return Nil
      }

    val addressList = source.getLines().map(_.trim).filter(_.matches(addressPattern)).toList
    source.close()

    addressList
  }

  private def getConfDir: String = {
    /* Get config directory path from JVM */
    if (sp("clean") != null) {
      // starting the program with fresh database
      println("Starting the server without any previous persistent state")

      // removing database folder
      val directory = new Directory(new File(dbPath))
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

  private def getDbDirectory: String = {
    val dbPathParam = "scala.db"
    val dbPath = sp(dbPathParam)
    if (dbPath != null && dbPath.trim.nonEmpty) {
      dbPath.trim + "/database"
    } else {
      "database"
    }
  }

  private def testMode: Boolean = {
    sp("test") != null
  }
}
