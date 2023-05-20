package ch.epfl.pop.config

import java.io.{BufferedWriter, File, FileWriter}

object RuntimeEnvironmentTestingHelper {

  private def ensureTestingMode(): Unit = {
    if (!RuntimeEnvironment.isTestMode) {
      throw new ExceptionInInitializerError("Mandatory argument \"-Dtest\" missing")
    }
  }

  def testWriteToServerPeersConfig(list: List[String]): Unit = {
    ensureTestingMode()
    val file = new BufferedWriter(new FileWriter(RuntimeEnvironment.serverPeersListPath))
    list.foreach {
      str =>
        file.write(str)
        file.newLine()
    }
    file.close()
  }

  def deleteTestConfig(): Unit = {
    ensureTestingMode()
    new File(RuntimeEnvironment.serverPeersListPath).delete()
  }

}
