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

  // Command line parameters
  private val cleanParam = "clean"
  private val configParam = "scala.config"
  private val dbPathParam = "scala.db"
  private val dbFolder = "database"
  private val testParam = "test"

  private lazy val sp = new SystemProperties()
  private lazy val confDir: String = getConfDir(cleanParam, configParam)
  private lazy val appConfFile = confDir + File.separator + "application.conf"

  lazy val dbPath: String = getDbDirectory(dbPathParam, dbFolder)
  lazy val appConf: Config = ConfigFactory.parseFile(new File(appConfFile))
  lazy val serverConf: ServerConf = ServerConf(appConf)

  lazy val ownRootPath = s"${serverConf.interface}:${serverConf.port}"
  lazy val ownClientAddress = f"ws://$ownRootPath/${serverConf.clientPath}"
  lazy val ownServerAddress = f"ws://$ownRootPath/${serverConf.serverPath}"
  lazy val ownAuthAddress = f"http://$ownRootPath/${serverConf.authenticationPath}"
  lazy val ownResponseAddress = f"ws://$ownRootPath/${serverConf.responseEndpoint}"
  lazy val ownPublicKeyAddress = f"http://$ownRootPath/${serverConf.publicKeyEndpoint}"

  // Needed for unit tests
  lazy val isTestMode: Boolean = testMode(testParam)
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

  private def getConfDir(cleanParam: String, configParam: String): String = {
    /* Get config directory path from JVM */
    if (sp(cleanParam) != null) {
      // starting the program with fresh database
      println("Starting the server without any previous persistent state")

      // removing database folder
      val directory = new Directory(new File(dbPath))
      if (directory.deleteRecursively()) {
        println("Removed old database folder")
      }
    }

    val pathConfig = sp(configParam)
    if (pathConfig != null && pathConfig.trim.nonEmpty) {
      pathConfig.trim
    } else {
      throw new RuntimeException(s"-D$configParam was not provided.")
    }
  }

  private def getDbDirectory(dbPathParam: String, dbFolder: String): String = {
    val dbPath = sp(dbPathParam)
    if (dbPath != null && dbPath.trim.nonEmpty) {
      dbPath.trim + File.separator + dbFolder
    } else {
      dbFolder
    }
  }

  private def testMode(testParam: String): Boolean = {
    sp(testParam) != null
  }
}
