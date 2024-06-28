package ch.epfl.pop.config

import com.typesafe.config.{Config, ConfigFactory}

import java.io.File
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
  private val securityParam = "scala.security"
  private val dbPathParam = "scala.db"
  private val peerListParam = "scala.peerlist"
  private val appConfParam = "scala.appConf"
  private val dbFolder = "database"
  private val testParam = "test"

  private lazy val sp = new SystemProperties()
  private lazy val confDir: String = getConfDir(cleanParam, configParam)
  private lazy val appConfFile = getAppConf

  lazy val dbPath: String = getDbDirectory(dbPathParam, dbFolder)
  lazy val securityPath: String = getRequiredDirectory(securityParam)
  lazy val appConf: Config = ConfigFactory.parseFile(new File(appConfFile))
  lazy val serverConf: ServerConf = ServerConf(appConf)

  lazy val ownRootPath: String = s"${serverConf.interface}:${serverConf.port}"
  lazy val ownClientAddress: String = f"ws://$ownRootPath/${serverConf.clientPath}"
  lazy val ownServerAddress: String = f"ws://$ownRootPath/${serverConf.serverPath}"
  lazy val ownAuthAddress: String = f"http://$ownRootPath/${serverConf.authenticationPath}"
  lazy val ownResponseAddress: String = f"ws://$ownRootPath/${serverConf.responseEndpoint}"
  lazy val ownPublicKeyAddress: String = f"http://$ownRootPath/${serverConf.publicKeyEndpoint}"

  // Needed for unit tests
  lazy val isTestMode: Boolean = testMode(testParam)
  lazy val serverPeersListPath: String = getPeerListPath()

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

    getRequiredDirectory(configParam)
  }

  private def getDbDirectory(dbPathParam: String, dbFolder: String): String = {
    val dbPath = sp(dbPathParam)
    if (dbPath != null && dbPath.trim.nonEmpty) {
      dbPath.trim + File.separator + dbFolder
    } else {
      dbFolder
    }
  }

  private def getRequiredDirectory(param: String): String = {
    val path = sp(param)
    if (path != null && path.trim.nonEmpty) {
      path.trim
    } else {
      throw new RuntimeException(s"-D$param was not provided.")
    }
  }

  private def getPeerListPath(): String = {
    val path = sp(peerListParam)
    if (path != null && path.trim.nonEmpty) {
      confDir + File.separator + path.trim
    } else if (isTestMode) {
      confDir + File.separator + "server-peers-list-mock.conf"
    } else {
      confDir + File.separator + "server-peers-list.conf"
    }
  }

  private def getAppConf: String = {
    val path = sp(appConfParam)
    if path != null && path.trim.nonEmpty then
      confDir + File.separator + path.trim
    else
      confDir + File.separator + "application.conf"
  }

  private def testMode(testParam: String): Boolean = {
    sp(testParam) != null
  }
}
