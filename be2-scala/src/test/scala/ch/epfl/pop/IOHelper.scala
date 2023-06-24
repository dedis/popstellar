package ch.epfl.pop

import scala.io.Source.fromFile

object IOHelper {

  /** Read a file from given path
    *
    * @param path
    *   path of the desired file
    * @return
    *   the string content of the file
    */
  def readJsonFromPath(path: String): String = {
    val source = fromFile(path)
    val jsonString: String = {
      try {
        source.mkString
      } finally {
        source.close()
      }
    }
    jsonString
  }

}
