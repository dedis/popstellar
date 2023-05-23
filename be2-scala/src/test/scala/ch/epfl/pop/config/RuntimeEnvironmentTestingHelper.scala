package ch.epfl.pop.config

import ch.epfl.pop.config.RuntimeEnvironment.serverPeersListPath

import java.io.{BufferedWriter, File, FileWriter}

object RuntimeEnvironmentTestingHelper {

  private def ensureTestingMode(): Unit = {
    if (!RuntimeEnvironment.isTestMode) {
      throw new ExceptionInInitializerError("Mandatory argument \"-Dtest\" missing")
    }
  }

  /** Write to the server-peers-mock.conf in tests only, the file is auto deleted after jvm shutdown
    *
    * @param list
    *   the list of strings to write in to the mock config file
    */
  def testWriteToServerPeersConfig(list: List[String]): Unit = {
    ensureTestingMode()
    val file = new BufferedWriter(new FileWriter(serverPeersListPath))
    list.foreach {
      str =>
        file.write(str)
        file.newLine()
    }
    file.close()

    // Set the file to delete itself on jvm shutdown
    new File(serverPeersListPath).deleteOnExit()
  }

}
